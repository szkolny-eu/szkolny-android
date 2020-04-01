/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-25. 
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikData
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web.IdziennikWebGetAttachment
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web.IdziennikWebGetMessage
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web.IdziennikWebGetRecipientList
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web.IdziennikWebSendMessage
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.firstlogin.IdziennikFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.login.IdziennikLogin
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.utils.Utils.d

class Idziennik(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        private const val TAG = "Idziennik"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataIdziennik
    private var afterLogin: (() -> Unit)? = null

    init {
        data = DataIdziennik(app, profile, loginStore).apply {
            callback = wrapCallback(this@Idziennik.callback)
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
    override fun sync(featureIds: List<Int>, viewId: Int?, onlyEndpoints: List<Int>?, arguments: JsonObject?) {
        data.arguments = arguments
        data.prepare(idziennikLoginMethods, IdziennikFeatures, featureIds, viewId, onlyEndpoints)
        login()
    }

    private fun login(loginMethodId: Int? = null, afterLogin: (() -> Unit)? = null) {
        d(TAG, "Trying to login with ${data.targetLoginMethodIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        loginMethodId?.let { data.prepareFor(idziennikLoginMethods, it) }
        afterLogin?.let { this.afterLogin = it }
        IdziennikLogin(data) {
            data()
        }
    }

    private fun data() {
        d(TAG, "Endpoint IDs: ${data.targetEndpointIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        afterLogin?.invoke() ?: IdziennikData(data) {
            completed()
        }
    }

    override fun getMessage(message: MessageFull) {
        login(LOGIN_METHOD_IDZIENNIK_WEB) {
            IdziennikWebGetMessage(data, message) {
                completed()
            }
        }
    }

    override fun sendMessage(recipients: List<Teacher>, subject: String, text: String) {
        login(LOGIN_METHOD_IDZIENNIK_API) {
            IdziennikWebSendMessage(data, recipients, subject, text) {
                completed()
            }
        }
    }

    override fun markAllAnnouncementsAsRead() {}
    override fun getAnnouncement(announcement: AnnouncementFull) {}

    override fun getAttachment(owner: Any, attachmentId: Long, attachmentName: String) {
        login(LOGIN_METHOD_IDZIENNIK_WEB) {
            IdziennikWebGetAttachment(data, owner, attachmentId, attachmentName) {
                completed()
            }
        }
    }

    override fun getRecipientList() {
        login(LOGIN_METHOD_IDZIENNIK_WEB) {
            IdziennikWebGetRecipientList(data) {
                completed()
            }
        }
    }

    override fun getEvent(eventFull: EventFull) {}

    override fun firstLogin() { IdziennikFirstLogin(data) { completed() } }
    override fun cancel() {
        d(TAG, "Cancelled")
        data.cancel()
        callback.onCompleted()
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
                    ERROR_LOGIN_IDZIENNIK_WEB_NO_SESSION,
                    ERROR_LOGIN_IDZIENNIK_WEB_NO_AUTH,
                    ERROR_LOGIN_IDZIENNIK_WEB_NO_BEARER,
                    ERROR_IDZIENNIK_WEB_ACCESS_DENIED,
                    ERROR_IDZIENNIK_API_ACCESS_DENIED -> {
                        data.loginMethods.remove(LOGIN_METHOD_IDZIENNIK_WEB)
                        data.prepareFor(idziennikLoginMethods, LOGIN_METHOD_IDZIENNIK_WEB)
                        data.loginExpiryTime = 0
                        login()
                    }
                    ERROR_IDZIENNIK_API_NO_REGISTER -> {
                        data()
                    }
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
