/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusData
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages.LibrusMessagesGetAttachment
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages.LibrusMessagesGetMessage
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia.LibrusSynergiaMarkAllAnnouncementsAsRead
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.firstlogin.LibrusFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.login.*
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.utils.Utils.d

class Librus(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        private const val TAG = "Librus"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataLibrus

    init {
        data = DataLibrus(app, profile, loginStore).apply {
            callback = wrapCallback(this@Librus.callback)
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
        data.prepare(librusLoginMethods, LibrusFeatures, featureIds, viewId)
        login()
    }

    private fun login() {
        d(TAG, "Trying to login with ${data.targetLoginMethodIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
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
        LibrusData(data) {
            completed()
        }
    }

    override fun getMessage(message: MessageFull) {
        LibrusLoginPortal(data) {
            LibrusLoginApi(data) {
                LibrusLoginSynergia(data) {
                    LibrusLoginMessages(data) {
                        LibrusMessagesGetMessage(data, message) {
                            completed()
                        }
                    }
                }
            }
        }
    }

    override fun markAllAnnouncementsAsRead() {
        LibrusLoginPortal(data) {
            LibrusLoginApi(data) {
                LibrusLoginSynergia(data) {
                    LibrusSynergiaMarkAllAnnouncementsAsRead(data) {
                        completed()
                    }
                }
            }
        }
    }

    override fun getAttachment(message: Message, attachmentId: Long, attachmentName: String) {
        LibrusLoginPortal(data) {
            LibrusLoginApi(data) {
                LibrusLoginSynergia(data) {
                    LibrusLoginMessages(data) {
                        LibrusMessagesGetAttachment(data, message, attachmentId, attachmentName) {
                            completed()
                        }
                    }
                }
            }
        }
    }

    override fun firstLogin() {
        LibrusFirstLogin(data) {
            completed()
        }
    }

    override fun cancel() {
        d(TAG, "Cancelled")
        data.cancel()
    }

    private fun wrapCallback(callback: EdziennikCallback): EdziennikCallback {
        return object : EdziennikCallback {
            override fun onCompleted() {
                callback.onCompleted()
            }

            override fun onProgress(step: Float) {
                callback.onProgress(step)
            }

            override fun onStartProgress(stringRes: Int) {
                callback.onStartProgress(stringRes)
            }

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
                        data.targetLoginMethodIds.add(LOGIN_METHOD_LIBRUS_PORTAL)
                        data.targetLoginMethodIds.sort()
                        data.portalTokenExpiryTime = 0
                        login()
                    }
                    ERROR_LIBRUS_API_ACCESS_DENIED,
                    ERROR_LIBRUS_API_TOKEN_EXPIRED -> {
                        data.loginMethods.remove(LOGIN_METHOD_LIBRUS_API)
                        data.targetLoginMethodIds.add(LOGIN_METHOD_LIBRUS_API)
                        data.targetLoginMethodIds.sort()
                        data.apiTokenExpiryTime = 0
                        login()
                    }
                    ERROR_LIBRUS_SYNERGIA_ACCESS_DENIED -> {
                        data.loginMethods.remove(LOGIN_METHOD_LIBRUS_SYNERGIA)
                        data.targetLoginMethodIds.add(LOGIN_METHOD_LIBRUS_SYNERGIA)
                        data.targetLoginMethodIds.sort()
                        data.synergiaSessionIdExpiryTime = 0
                        login()
                    }
                    ERROR_LIBRUS_MESSAGES_ACCESS_DENIED -> {
                        data.loginMethods.remove(LOGIN_METHOD_LIBRUS_MESSAGES)
                        data.targetLoginMethodIds.add(LOGIN_METHOD_LIBRUS_MESSAGES)
                        data.targetLoginMethodIds.sort()
                        data.messagesSessionIdExpiryTime = 0
                        login()
                    }
                    ERROR_LOGIN_LIBRUS_PORTAL_NO_CODE,
                    ERROR_LOGIN_LIBRUS_PORTAL_CSRF_MISSING,
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
                    // TODO PORTAL CAPTCHA
                    ERROR_LIBRUS_API_TIMETABLE_NOT_PUBLIC -> {
                        data.timetableNotPublic = true
                        data()
                    }
                    ERROR_LIBRUS_API_LUCKY_NUMBER_NOT_ACTIVE,
                    ERROR_LIBRUS_API_NOTES_NOT_ACTIVE -> {
                        data()
                    }
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
