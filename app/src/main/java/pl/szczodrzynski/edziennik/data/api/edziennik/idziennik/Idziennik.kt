/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-25. 
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.CODE_INTERNAL_LIBRUS_ACCOUNT_410
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikData
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web.IdziennikWebGetAttachment
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web.IdziennikWebGetMessage
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.firstlogin.IdziennikFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.login.IdziennikLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.login.IdziennikLoginWeb
import pl.szczodrzynski.edziennik.data.api.idziennikLoginMethods
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.prepare
import pl.szczodrzynski.edziennik.data.db.modules.announcements.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.utils.Utils.d

class Idziennik(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        private const val TAG = "Idziennik"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataIdziennik

    init {
        data = DataIdziennik(app, profile, loginStore).apply {
            callback = wrapCallback(this@Idziennik.callback)
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
        data.prepare(idziennikLoginMethods, IdziennikFeatures, featureIds, viewId)
        d(TAG, "LoginMethod IDs: ${data.targetLoginMethodIds}")
        d(TAG, "Endpoint IDs: ${data.targetEndpointIds}")
        IdziennikLogin(data) {
            IdziennikData(data) {
                completed()
            }
        }
    }

    override fun getMessage(message: MessageFull) {
        IdziennikLoginWeb(data) {
            IdziennikWebGetMessage(data, message) {
                completed()
            }
        }
    }

    override fun markAllAnnouncementsAsRead() {

    }

    override fun getAnnouncement(announcement: AnnouncementFull) {

    }

    override fun getAttachment(message: Message, attachmentId: Long, attachmentName: String) {
        IdziennikLoginWeb(data) {
            IdziennikWebGetAttachment(data, message, attachmentId, attachmentName) {
                completed()
            }
        }
    }

    override fun firstLogin() {
        IdziennikFirstLogin(data) {
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
                when (apiError.errorCode) {
                    in internalErrorList -> {
                        // finish immediately if the same error occurs twice during the same sync
                        callback.onError(apiError)
                    }
                    CODE_INTERNAL_LIBRUS_ACCOUNT_410 -> {
                        internalErrorList.add(apiError.errorCode)
                        loginStore.removeLoginData("refreshToken") // force a clean login
                        //loginLibrus()
                    }
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
