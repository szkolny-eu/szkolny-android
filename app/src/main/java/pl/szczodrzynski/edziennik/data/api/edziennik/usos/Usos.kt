/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosData
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.firstlogin.UsosFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.login.UsosLogin
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.prepare
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.utils.Utils.d

class Usos(
    val app: App,
    val profile: Profile?,
    val loginStore: LoginStore,
    val callback: EdziennikCallback,
) : EdziennikInterface {
    companion object {
        private const val TAG = "Usos"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataUsos

    init {
        data = DataUsos(app, profile, loginStore).apply {
            callback = wrapCallback(this@Usos.callback)
            satisfyLoginMethods()
        }
    }

    private fun completed() {
        data.saveData()
        callback.onCompleted()
    }

    override fun sync(
        featureTypes: Set<FeatureType>?,
        viewId: Int?,
        onlyEndpoints: List<Int>?,
        arguments: JsonObject?,
    ) {
        data.arguments = arguments
        data.prepare(UsosFeatures, featureTypes, viewId, onlyEndpoints)
        d(TAG, "LoginMethod IDs: ${data.targetLoginMethods}")
        d(TAG, "Endpoint IDs: ${data.targetEndpoints}")
        UsosLogin(data) {
            UsosData(data) {
                completed()
            }
        }
    }

    override fun getMessage(message: MessageFull) {}
    override fun sendMessage(recipients: List<Teacher>, subject: String, text: String) {}
    override fun markAllAnnouncementsAsRead() {}
    override fun getAnnouncement(announcement: AnnouncementFull) {}
    override fun getAttachment(owner: Any, attachmentId: Long, attachmentName: String) {}
    override fun getRecipientList() {}
    override fun getEvent(eventFull: EventFull) {}

    override fun firstLogin() {
        UsosFirstLogin(data) {
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

            override fun onRequiresUserAction(event: UserActionRequiredEvent) {
                callback.onRequiresUserAction(event)
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
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
