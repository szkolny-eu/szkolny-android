/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikData
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web.EdudziennikWebGetAnnouncement
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.firstlogin.EdudziennikFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.login.EdudziennikLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.login.EdudziennikLoginWeb
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.utils.Utils.d

class Edudziennik(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        private const val TAG = "Edudziennik"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataEdudziennik
    private var afterLogin: (() -> Unit)? = null

    init {
        data = DataEdudziennik(app, profile, loginStore).apply {
            callback = wrapCallback(this@Edudziennik.callback)
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
    override fun sync(featureIds: List<Int>, viewId: Int?, arguments: JsonObject?) {
        data.arguments = arguments
        data.prepare(edudziennikLoginMethods, EdudziennikFeatures, featureIds, viewId)
        login()
    }

    private fun login(loginMethodId: Int? = null, afterLogin: (() -> Unit)? = null) {
        d(TAG, "Trying to login with ${data.targetLoginMethodIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        loginMethodId?.let { data.prepareFor(edudziennikLoginMethods, it) }
        afterLogin?.let { this.afterLogin = it }
        EdudziennikLogin(data) {
            data()
        }
    }

    private fun data() {
        d(TAG, "Endpoint IDs: ${data.targetEndpointIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        afterLogin?.invoke() ?: EdudziennikData(data) {
            completed()
        }
    }

    override fun getMessage(message: MessageFull) {}
    override fun sendMessage(recipients: List<Teacher>, subject: String, text: String) {}
    override fun markAllAnnouncementsAsRead() {}

    override fun getAnnouncement(announcement: AnnouncementFull) {
        EdudziennikLoginWeb(data) {
            EdudziennikWebGetAnnouncement(data, announcement) {
                completed()
            }
        }
    }

    override fun getAttachment(message: Message, attachmentId: Long, attachmentName: String) {}
    override fun getRecipientList() {}

    override fun firstLogin() { EdudziennikFirstLogin(data) { completed() } }
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
                    ERROR_EDUDZIENNIK_WEB_SESSION_EXPIRED -> {
                        login()
                    }
                    ERROR_LOGIN_EDUDZIENNIK_WEB_NO_SESSION_ID -> {
                        login()
                    }
                    ERROR_EDUDZIENNIK_WEB_LIMITED_ACCESS -> {
                        data()
                    }
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
