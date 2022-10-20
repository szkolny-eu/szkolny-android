/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-16.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik

import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.ERROR_PROFILE_ARCHIVED
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.Librus
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.Mobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.Podlasie
import pl.szczodrzynski.edziennik.data.api.edziennik.template.Template
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.Usos
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.Vulcan
import pl.szczodrzynski.edziennik.data.api.events.RegisterAvailabilityEvent
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.task.IApiTask
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.managers.AvailabilityManager.Error.Type

open class EdziennikTask(override val profileId: Int, val request: Any) : IApiTask(profileId) {
    companion object {
        private const val TAG = "EdziennikTask"

        var profile: Profile? = null
        var loginStore: LoginStore? = null

        fun firstLogin(loginStore: LoginStore) = EdziennikTask(-1, FirstLoginRequest(loginStore))
        fun sync() = EdziennikTask(-1, SyncRequest())
        fun syncProfile(profileId: Int, featureTypes: Set<FeatureType>? = null, onlyEndpoints: List<Int>? = null, arguments: JsonObject? = null) = EdziennikTask(profileId, SyncProfileRequest(featureTypes, onlyEndpoints, arguments))
        fun syncProfileList(profileList: List<Int>) = EdziennikTask(-1, SyncProfileListRequest(profileList))
        fun messageGet(profileId: Int, message: MessageFull) = EdziennikTask(profileId, MessageGetRequest(message))
        fun messageSend(profileId: Int, recipients: List<Teacher>, subject: String, text: String) = EdziennikTask(profileId, MessageSendRequest(recipients, subject, text))
        fun announcementsRead(profileId: Int) = EdziennikTask(profileId, AnnouncementsReadRequest())
        fun announcementGet(profileId: Int, announcement: AnnouncementFull) = EdziennikTask(profileId, AnnouncementGetRequest(announcement))
        fun attachmentGet(profileId: Int, owner: Any, attachmentId: Long, attachmentName: String) = EdziennikTask(profileId, AttachmentGetRequest(owner, attachmentId, attachmentName))
        fun recipientListGet(profileId: Int) = EdziennikTask(profileId, RecipientListGetRequest())
        fun eventGet(profileId: Int, event: EventFull) = EdziennikTask(profileId, EventGetRequest(event))
    }

    private lateinit var loginStore: LoginStore

    override fun prepare(app: App) {
        if (request is FirstLoginRequest) {
            // get the requested profile and login store
            this.profile = null
            loginStore = request.loginStore
            // save the profile ID and name as the current task's
            taskName = app.getString(R.string.edziennik_notification_api_first_login_title)
        } else {
            // get the requested profile and login store
            val profile = app.db.profileDao().getByIdNow(profileId) ?: return
            this.profile = profile
            val loginStore = app.db.loginStoreDao().getByIdNow(profile.loginStoreId) ?: return
            this.loginStore = loginStore
            // save the profile ID and name as the current task's
            taskName = app.getString(R.string.edziennik_notification_api_sync_title_format, profile.name)
        }
        EdziennikTask.profile = this.profile
        EdziennikTask.loginStore = this.loginStore
    }

    private var edziennikInterface: EdziennikInterface? = null

    internal fun run(app: App, taskCallback: EdziennikCallback) {
        profile?.let { profile ->
            if (profile.archived) {
                d(TAG, "The profile $profileId is archived")
                taskCallback.onError(ApiError(TAG, ERROR_PROFILE_ARCHIVED))
                return
            }
            else if (profile.shouldArchive()) {
                d(TAG, "The profile $profileId's year ended on ${profile.dateYearEnd}, archiving")
                ProfileArchiver(app, profile)
            }
            if (profile.isBeforeYear()) {
                d(TAG, "The profile $profileId's school year has not started yet; aborting sync")
                cancel()
                taskCallback.onCompleted()
                return
            }

            val error = app.availabilityManager.check(profile)
            when (error?.type) {
                Type.NOT_AVAILABLE -> {
                    if (EventBus.getDefault().hasSubscriberForEvent(RegisterAvailabilityEvent::class.java)) {
                        EventBus.getDefault().postSticky(RegisterAvailabilityEvent())
                    }
                    cancel()
                    taskCallback.onCompleted()
                    return
                }
                Type.API_ERROR -> {
                    taskCallback.onError(error.apiError!!)
                    return
                }
                else -> return@let
            }
        }

        edziennikInterface = when (loginStore.type) {
            LoginType.LIBRUS -> Librus(app, profile, loginStore, taskCallback)
            LoginType.MOBIDZIENNIK -> Mobidziennik(app, profile, loginStore, taskCallback)
            LoginType.VULCAN -> Vulcan(app, profile, loginStore, taskCallback)
            LoginType.PODLASIE -> Podlasie(app, profile, loginStore, taskCallback)
            LoginType.TEMPLATE -> Template(app, profile, loginStore, taskCallback)
            LoginType.USOS -> Usos(app, profile, loginStore, taskCallback)
            else -> null
        }
        if (edziennikInterface == null) {
            return
        }

        when (request) {
            is SyncProfileRequest -> edziennikInterface?.sync(
                    featureTypes = request.featureTypes,
                    onlyEndpoints = request.onlyEndpoints,
                    arguments = request.arguments)
            is MessageGetRequest -> edziennikInterface?.getMessage(request.message)
            is MessageSendRequest -> edziennikInterface?.sendMessage(request.recipients, request.subject, request.text)
            is FirstLoginRequest -> edziennikInterface?.firstLogin()
            is AnnouncementsReadRequest -> edziennikInterface?.markAllAnnouncementsAsRead()
            is AnnouncementGetRequest -> edziennikInterface?.getAnnouncement(request.announcement)
            is AttachmentGetRequest -> edziennikInterface?.getAttachment(request.owner, request.attachmentId, request.attachmentName)
            is RecipientListGetRequest -> edziennikInterface?.getRecipientList()
            is EventGetRequest -> edziennikInterface?.getEvent(request.event)
        }
    }

    override fun cancel() {
        d(TAG, "Task ${toString()} cancelling...")
        edziennikInterface?.cancel()
    }

    override fun toString(): String {
        return "EdziennikTask(profileId=$profileId, request=$request, edziennikInterface=$edziennikInterface)"
    }

    data class FirstLoginRequest(val loginStore: LoginStore)
    class SyncRequest
    data class SyncProfileRequest(val featureTypes: Set<FeatureType>? = null, val onlyEndpoints: List<Int>? = null, val arguments: JsonObject? = null)
    data class SyncProfileListRequest(val profileList: List<Int>)
    data class MessageGetRequest(val message: MessageFull)
    data class MessageSendRequest(val recipients: List<Teacher>, val subject: String, val text: String)
    class AnnouncementsReadRequest
    data class AnnouncementGetRequest(val announcement: AnnouncementFull)
    data class AttachmentGetRequest(val owner: Any, val attachmentId: Long, val attachmentName: String)
    class RecipientListGetRequest
    data class EventGetRequest(val event: EventFull)
}
