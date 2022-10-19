/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-12
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie

import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.edziennik.helper.DownloadAttachment
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.PodlasieData
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.firstlogin.PodlasieFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.login.PodlasieLogin
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent
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
import pl.szczodrzynski.edziennik.utils.Utils
import java.io.File

class Podlasie(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        const val TAG = "Podlasie"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataPodlasie

    init {
        data = DataPodlasie(app, profile, loginStore).apply {
            callback = wrapCallback(this@Podlasie.callback)
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
        data.prepare(PodlasieFeatures, featureTypes, viewId, onlyEndpoints)
        Utils.d(TAG, "LoginMethod IDs: ${data.targetLoginMethods}")
        Utils.d(TAG, "Endpoint IDs: ${data.targetEndpoints}")
        PodlasieLogin(data) {
            PodlasieData(data) {
                completed()
            }
        }
    }

    override fun getMessage(message: MessageFull) {

    }

    override fun sendMessage(recipients: List<Teacher>, subject: String, text: String) {

    }

    override fun markAllAnnouncementsAsRead() {

    }

    override fun getAnnouncement(announcement: AnnouncementFull) {

    }

    override fun getAttachment(owner: Any, attachmentId: Long, attachmentName: String) {
        val fileUrl = attachmentName.substringAfter(":")
        DownloadAttachment(fileUrl,
                onSuccess = { file ->
                    val event = AttachmentGetEvent(
                            data.profileId,
                            owner,
                            attachmentId,
                            AttachmentGetEvent.TYPE_FINISHED,
                            file.absolutePath
                    )

                    val attachmentDataFile = File(Utils.getStorageDir(), ".${data.profileId}_${event.ownerId}_${event.attachmentId}")
                    Utils.writeStringToFile(attachmentDataFile, event.fileName)

                    EventBus.getDefault().postSticky(event)

                    completed()
                },
                onProgress = { written, _ ->
                    val event = AttachmentGetEvent(
                            data.profileId,
                            owner,
                            attachmentId,
                            AttachmentGetEvent.TYPE_PROGRESS,
                            bytesWritten = written
                    )

                    EventBus.getDefault().postSticky(event)
                },
                onError = { apiError ->
                    data.error(apiError)
                })
    }

    override fun getRecipientList() {

    }

    override fun getEvent(eventFull: EventFull) {

    }

    override fun firstLogin() {
        PodlasieFirstLogin(data) {
            completed()
        }
    }

    override fun cancel() {
        Utils.d(TAG, "Cancelled")
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
                // TODO Error handling
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
