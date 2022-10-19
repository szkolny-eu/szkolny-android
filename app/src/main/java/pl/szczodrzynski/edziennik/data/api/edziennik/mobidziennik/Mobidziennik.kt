/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikData
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web.*
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.firstlogin.MobidziennikFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.login.MobidziennikLogin
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.utils.Utils.d

class Mobidziennik(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        private const val TAG = "Mobidziennik"

        const val API_KEY = "szkolny_eu_72c7dbc8b97f1e5dd2d118cacf51c2b8543d15c0f65b7a59979adb0a1296b235d7febb826dd2a28688def6efe0811b924b04d7f3c7b7d005354e06dc56815d57"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataMobidziennik
    private var afterLogin: (() -> Unit)? = null

    init {
        data = DataMobidziennik(app, profile, loginStore).apply {
            callback = wrapCallback(this@Mobidziennik.callback)
            satisfyLoginMethods()
        }
    }

    private fun completed() {
        data.saveData()
        callback.onCompleted()
    }

    /*    _______ _                     _                  _ _   _
         |__   __| |              /\   | |                (_) | | |
            | |  | |__   ___     /  \  | | __ _  ___  _ __ _| |_| |__  _ __ ___
            | |  | '_ \ / _ \   / /\ \ | |/ _` |/ _ \| '__| | __| '_ \| '_ ` _ \
            | |  | | | |  __/  / ____ \| | (_| | (_) | |  | | |_| | | | | | | | |
            |_|  |_| |_|\___| /_/    \_\_|\__, |\___/|_|  |_|\__|_| |_|_| |_| |_|
                                           __/ |
                                          |__*/
    override fun sync(featureTypes: Set<FeatureType>?, viewId: Int?, onlyEndpoints: List<Int>?, arguments: JsonObject?) {
        data.arguments = arguments
        data.prepare(MobidziennikFeatures, featureTypes, viewId, onlyEndpoints)
        login()
    }

    private fun login(loginMethod: LoginMethod? = null, afterLogin: (() -> Unit)? = null) {
        d(TAG, "Trying to login with ${data.targetLoginMethods}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        loginMethod?.let { data.prepareFor(it) }
        afterLogin?.let { this.afterLogin = it }
        MobidziennikLogin(data) {
            data()
        }
    }

    private fun data() {
        d(TAG, "Endpoint IDs: ${data.targetEndpoints}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        afterLogin?.invoke() ?: MobidziennikData(data) {
            completed()
        }
    }

    override fun getMessage(message: MessageFull) {
        login(LoginMethod.MOBIDZIENNIK_WEB) {
            MobidziennikWebGetMessage(data, message) {
                completed()
            }
        }
    }

    override fun sendMessage(recipients: List<Teacher>, subject: String, text: String) {
        login(LoginMethod.MOBIDZIENNIK_WEB) {
            MobidziennikWebSendMessage(data, recipients, subject, text) {
                completed()
            }
        }
    }

    override fun markAllAnnouncementsAsRead() {}
    override fun getAnnouncement(announcement: AnnouncementFull) {}

    override fun getAttachment(owner: Any, attachmentId: Long, attachmentName: String) {
        login(LoginMethod.MOBIDZIENNIK_WEB) {
            MobidziennikWebGetAttachment(data, owner, attachmentId, attachmentName) {
                completed()
            }
        }
    }

    override fun getRecipientList() {
        login(LoginMethod.MOBIDZIENNIK_WEB) {
            MobidziennikWebGetRecipientList(data) {
                completed()
            }
        }
    }

    override fun getEvent(eventFull: EventFull) {
        login(LoginMethod.MOBIDZIENNIK_WEB) {
            if (eventFull.isHomework) {
                MobidziennikWebGetHomework(data, eventFull) {
                    completed()
                }
            }
            else {
                MobidziennikWebGetEvent(data, eventFull) {
                    completed()
                }
            }
        }
    }

    override fun firstLogin() { MobidziennikFirstLogin(data) { completed() } }
    override fun cancel() {
        d(TAG, "Cancelled")
        data.cancel()
    }

    private fun wrapCallback(callback: EdziennikCallback): EdziennikCallback {
        return object : EdziennikCallback {
            override fun onCompleted() { callback.onCompleted() }
            override fun onRequiresUserAction(event: UserActionRequiredEvent) { callback.onRequiresUserAction(event) }
            override fun onProgress(step: Float) { callback.onProgress(step) }
            override fun onStartProgress(stringRes: Int) { callback.onStartProgress(stringRes) }
            override fun onError(apiError: ApiError) {
                if (apiError.errorCode in internalErrorList) {
                    // finish immediately if the same error occurs twice during the same sync
                    callback.onError(apiError)
                    return
                }
                internalErrorList.add(apiError.errorCode)
                when (apiError.errorCode) {
                    ERROR_MOBIDZIENNIK_WEB_ACCESS_DENIED,
                    ERROR_MOBIDZIENNIK_WEB_NO_SESSION_KEY,
                    ERROR_MOBIDZIENNIK_WEB_NO_SESSION_VALUE,
                    ERROR_MOBIDZIENNIK_WEB_NO_SERVER_ID -> {
                        data.loginMethods.remove(LoginMethod.MOBIDZIENNIK_WEB)
                        data.prepareFor(LoginMethod.MOBIDZIENNIK_WEB)
                        data.webSessionIdExpiryTime = 0
                        login()
                    }
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
