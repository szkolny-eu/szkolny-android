/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusData
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api.LibrusApiAnnouncementMarkAsRead
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages.LibrusMessagesGetAttachment
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages.LibrusMessagesGetMessage
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages.LibrusMessagesGetRecipientList
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages.LibrusMessagesSendMessage
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia.LibrusSynergiaMarkAllAnnouncementsAsRead
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.firstlogin.LibrusFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.login.LibrusLogin
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.utils.Utils.d

class Librus(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        private const val TAG = "Librus"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataLibrus
    private var afterLogin: (() -> Unit)? = null

    init {
        data = DataLibrus(app, profile, loginStore).apply {
            callback = wrapCallback(this@Librus.callback)
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
        data.prepare(librusLoginMethods, LibrusFeatures, featureIds, viewId, onlyEndpoints)
        login()
    }

    private fun login(loginMethodId: Int? = null, afterLogin: (() -> Unit)? = null) {
        d(TAG, "Trying to login with ${data.targetLoginMethodIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        loginMethodId?.let { data.prepareFor(librusLoginMethods, it) }
        afterLogin?.let { this.afterLogin = it }
        LibrusLogin(data) {
            data()
        }
    }

    private fun data() {
        d(TAG, "Endpoint IDs: ${data.targetEndpointIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        afterLogin?.invoke() ?: LibrusData(data) {
            completed()
        }
    }

    override fun getMessage(message: MessageFull) {
        login(LOGIN_METHOD_LIBRUS_MESSAGES) {
            LibrusMessagesGetMessage(data, message) {
                completed()
            }
        }
    }

    override fun sendMessage(recipients: List<Teacher>, subject: String, text: String) {
        login(LOGIN_METHOD_LIBRUS_MESSAGES) {
            LibrusMessagesSendMessage(data, recipients, subject, text) {
                completed()
            }
        }
    }

    override fun markAllAnnouncementsAsRead() {
        login(LOGIN_METHOD_LIBRUS_SYNERGIA) {
            LibrusSynergiaMarkAllAnnouncementsAsRead(data) {
                completed()
            }
        }
    }

    override fun getAnnouncement(announcement: AnnouncementFull) {
        login(LOGIN_METHOD_LIBRUS_API) {
            LibrusApiAnnouncementMarkAsRead(data, announcement) {
                completed()
            }
        }
    }

    override fun getAttachment(message: Message, attachmentId: Long, attachmentName: String) {
        login(LOGIN_METHOD_LIBRUS_MESSAGES) {
            LibrusMessagesGetAttachment(data, message, attachmentId, attachmentName) {
                completed()
            }
        }
    }

    override fun getRecipientList() {
        login(LOGIN_METHOD_LIBRUS_MESSAGES) {
            LibrusMessagesGetRecipientList(data) {
                completed()
            }
        }
    }

    override fun getEvent(eventFull: EventFull) {}

    override fun firstLogin() { LibrusFirstLogin(data) { completed() } }
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
                    ERROR_LIBRUS_PORTAL_ACCESS_DENIED -> {
                        data.loginMethods.remove(LOGIN_METHOD_LIBRUS_PORTAL)
                        data.prepareFor(librusLoginMethods, LOGIN_METHOD_LIBRUS_PORTAL)
                        data.portalTokenExpiryTime = 0
                        login()
                    }
                    ERROR_LIBRUS_API_ACCESS_DENIED,
                    ERROR_LIBRUS_API_TOKEN_EXPIRED -> {
                        data.loginMethods.remove(LOGIN_METHOD_LIBRUS_API)
                        data.prepareFor(librusLoginMethods, LOGIN_METHOD_LIBRUS_API)
                        data.apiTokenExpiryTime = 0
                        login()
                    }
                    ERROR_LIBRUS_SYNERGIA_ACCESS_DENIED -> {
                        data.loginMethods.remove(LOGIN_METHOD_LIBRUS_SYNERGIA)
                        data.prepareFor(librusLoginMethods, LOGIN_METHOD_LIBRUS_SYNERGIA)
                        data.synergiaSessionIdExpiryTime = 0
                        login()
                    }
                    ERROR_LIBRUS_MESSAGES_ACCESS_DENIED -> {
                        data.loginMethods.remove(LOGIN_METHOD_LIBRUS_MESSAGES)
                        data.prepareFor(librusLoginMethods, LOGIN_METHOD_LIBRUS_MESSAGES)
                        data.messagesSessionIdExpiryTime = 0
                        login()
                    }
                    ERROR_LOGIN_LIBRUS_PORTAL_NO_CODE,
                    ERROR_LOGIN_LIBRUS_PORTAL_CSRF_MISSING,
                    ERROR_LOGIN_LIBRUS_PORTAL_CSRF_EXPIRED,
                    ERROR_LOGIN_LIBRUS_PORTAL_CODE_REVOKED,
                    ERROR_LOGIN_LIBRUS_PORTAL_CODE_EXPIRED -> {
                        login()
                    }
                    ERROR_LOGIN_LIBRUS_PORTAL_NO_REFRESH,
                    ERROR_LOGIN_LIBRUS_PORTAL_REFRESH_REVOKED,
                    ERROR_LOGIN_LIBRUS_PORTAL_REFRESH_INVALID -> {
                        data.portalRefreshToken = null
                        login()
                    }
                    ERROR_LOGIN_LIBRUS_SYNERGIA_TOKEN_INVALID,
                    ERROR_LOGIN_LIBRUS_SYNERGIA_NO_TOKEN,
                    ERROR_LOGIN_LIBRUS_SYNERGIA_NO_SESSION_ID -> {
                        login()
                    }
                    ERROR_LOGIN_LIBRUS_MESSAGES_NO_SESSION_ID -> {
                        login()
                    }
                    ERROR_LIBRUS_API_TIMETABLE_NOT_PUBLIC -> {
                        data.timetableNotPublic = true
                        data()
                    }
                    ERROR_LIBRUS_API_LUCKY_NUMBER_NOT_ACTIVE,
                    ERROR_LIBRUS_API_NOTES_NOT_ACTIVE -> {
                        data()
                    }
                    ERROR_LIBRUS_API_DEVICE_REGISTERED -> {
                        data.app.config.sync.tokenLibrusList =
                                data.app.config.sync.tokenLibrusList + data.profileId
                        data()
                    }
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
