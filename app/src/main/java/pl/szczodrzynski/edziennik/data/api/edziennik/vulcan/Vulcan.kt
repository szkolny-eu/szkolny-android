/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6. 
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan

import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.ERROR_ONEDRIVE_DOWNLOAD
import pl.szczodrzynski.edziennik.data.api.ERROR_VULCAN_API_DEPRECATED
import pl.szczodrzynski.edziennik.data.api.edziennik.helper.OneDriveDownloadAttachment
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanData
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe.VulcanHebeMessagesChangeStatus
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe.VulcanHebeSendMessage
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.firstlogin.VulcanFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login.VulcanLogin
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent
import pl.szczodrzynski.edziennik.data.api.events.EventGetEvent
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.prepare
import pl.szczodrzynski.edziennik.data.api.prepareFor
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.db.enums.LoginMode
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.io.File

class Vulcan(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        private const val TAG = "Vulcan"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataVulcan
    private var afterLogin: (() -> Unit)? = null

    init {
        data = DataVulcan(app, profile, loginStore).apply {
            callback = wrapCallback(this@Vulcan.callback)
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
    override fun sync(featureTypes: Set<FeatureType>?, onlyEndpoints: Set<Int>?, arguments: JsonObject?) {
        data.arguments = arguments
        data.prepare(VulcanFeatures, featureTypes, onlyEndpoints)
        login()
    }

    private fun login(loginMethod: LoginMethod? = null, afterLogin: (() -> Unit)? = null) {
        if (data.loginStore.mode == LoginMode.VULCAN_API) {
            data.error(TAG, ERROR_VULCAN_API_DEPRECATED)
            return
        }

        d(TAG, "Trying to login with ${data.targetLoginMethods}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        loginMethod?.let { data.prepareFor(it) }
        afterLogin?.let { this.afterLogin = it }
        VulcanLogin(data) {
            data()
        }
    }

    private fun data() {
        d(TAG, "Endpoint IDs: ${data.targetEndpoints}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        afterLogin?.invoke() ?: VulcanData(data) {
            completed()
        }
    }

    override fun getMessage(message: MessageFull) {
        login(LoginMethod.VULCAN_HEBE) {
            if (message.seen) {
                EventBus.getDefault().postSticky(MessageGetEvent(message))
                completed()
                return@login
            }
            VulcanHebeMessagesChangeStatus(data, message) {
                completed()
            }
        }
    }

    override fun sendMessage(recipients: Set<Teacher>, subject: String, text: String) {
        login(LoginMethod.VULCAN_HEBE) {
            VulcanHebeSendMessage(data, recipients, subject, text) {
                completed()
            }
        }
    }

    override fun markAllAnnouncementsAsRead() {}
    override fun getAnnouncement(announcement: AnnouncementFull) {}
    override fun getRecipientList() {}

    override fun getAttachment(owner: Any, attachmentId: Long, attachmentName: String) {
        val fileUrl = attachmentName.substringAfter(":")
        if (attachmentName == fileUrl) {
            data.error(ApiError(TAG, ERROR_ONEDRIVE_DOWNLOAD))
            return
        }

        OneDriveDownloadAttachment(
                app,
                fileUrl,
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
                }
        )
    }

    override fun getEvent(eventFull: EventFull) {
        eventFull.homeworkBody = ""
        eventFull.isDownloaded = true

        EventBus.getDefault().postSticky(EventGetEvent(eventFull))
        completed()
    }

    override fun firstLogin() { VulcanFirstLogin(data) { completed() } }
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
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
