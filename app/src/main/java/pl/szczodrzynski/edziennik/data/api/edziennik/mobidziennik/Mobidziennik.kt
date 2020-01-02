/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikData
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web.MobidziennikWebGetAttachment
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web.MobidziennikWebGetMessage
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web.MobidziennikWebGetRecipientList
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web.MobidziennikWebSendMessage
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.firstlogin.MobidziennikFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.login.MobidziennikLogin
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.announcements.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
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
        data.notify {
            callback.onCompleted()
        }
    }

    /*    _______ _                     _                  _ _   _
         |__   __| |              /\   | |                (_) | | |
            | |  | |__   ___     /  \  | | __ _  ___  _ __ _| |_| |__  _ __ ___
            | |  | '_ \ / _ \   / /\ \ | |/ _` |/ _ \| '__| | __| '_ \| '_ ` _ \
            | |  | | | |  __/  / ____ \| | (_| | (_) | |  | | |_| | | | | | | | |
            |_|  |_| |_|\___| /_/    \_\_|\__, |\___/|_|  |_|\__|_| |_|_| |_| |_|
                                           __/ |
                                          |__*/
    override fun sync(featureIds: List<Int>, viewId: Int?, arguments: JsonObject?) {
        data.arguments = arguments
        data.prepare(mobidziennikLoginMethods, MobidziennikFeatures, featureIds, viewId)
        login()
    }

    private fun login(loginMethodId: Int? = null, afterLogin: (() -> Unit)? = null) {
        d(TAG, "Trying to login with ${data.targetLoginMethodIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        loginMethodId?.let { data.prepareFor(mobidziennikLoginMethods, it) }
        afterLogin?.let { this.afterLogin = it }
        MobidziennikLogin(data) {
            data()
        }
    }

    private fun data() {
        d(TAG, "Endpoint IDs: ${data.targetEndpointIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        afterLogin?.invoke() ?: MobidziennikData(data) {
            completed()
        }
    }

    override fun getMessage(message: MessageFull) {
        login(LOGIN_METHOD_MOBIDZIENNIK_WEB) {
            MobidziennikWebGetMessage(data, message) {
                completed()
            }
        }
    }

    override fun sendMessage(recipients: List<Teacher>, subject: String, text: String) {
        login(LOGIN_METHOD_MOBIDZIENNIK_WEB) {
            MobidziennikWebSendMessage(data, recipients, subject, text) {
                completed()
            }
        }
    }

    override fun markAllAnnouncementsAsRead() {}
    override fun getAnnouncement(announcement: AnnouncementFull) {}

    override fun getAttachment(message: Message, attachmentId: Long, attachmentName: String) {
        login(LOGIN_METHOD_MOBIDZIENNIK_WEB) {
            MobidziennikWebGetAttachment(data, message, attachmentId, attachmentName) {
                completed()
            }
        }
    }

    override fun getRecipientList() {
        login(LOGIN_METHOD_MOBIDZIENNIK_WEB) {
            MobidziennikWebGetRecipientList(data) {
                completed()
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
                        data.loginMethods.remove(LOGIN_METHOD_MOBIDZIENNIK_WEB)
                        data.prepareFor(mobidziennikLoginMethods, LOGIN_METHOD_MOBIDZIENNIK_WEB)
                        data.webSessionIdExpiryTime = 0
                        login()
                    }
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
