/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-16.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.Edudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.Idziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.Librus
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.Mobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.template.Template
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.Vulcan
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.task.IApiTask
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull

open class EdziennikTask(override val profileId: Int, val request: Any) : IApiTask(profileId) {
    companion object {
        private const val TAG = "EdziennikTask"

        fun firstLogin(loginStore: LoginStore) = EdziennikTask(-1, FirstLoginRequest(loginStore))
        fun sync() = EdziennikTask(-1, SyncRequest())
        fun syncProfile(profileId: Int, viewIds: List<Pair<Int, Int>>? = null, onlyEndpoints: List<Int>? = null, arguments: JsonObject? = null) = EdziennikTask(profileId, SyncProfileRequest(viewIds, onlyEndpoints, arguments))
        fun syncProfileList(profileList: List<Int>) = EdziennikTask(-1, SyncProfileListRequest(profileList))
        fun messageGet(profileId: Int, message: MessageFull) = EdziennikTask(profileId, MessageGetRequest(message))
        fun messageSend(profileId: Int, recipients: List<Teacher>, subject: String, text: String) = EdziennikTask(profileId, MessageSendRequest(recipients, subject, text))
        fun announcementsRead(profileId: Int) = EdziennikTask(profileId, AnnouncementsReadRequest())
        fun announcementGet(profileId: Int, announcement: AnnouncementFull) = EdziennikTask(profileId, AnnouncementGetRequest(announcement))
        fun attachmentGet(profileId: Int, message: Message, attachmentId: Long, attachmentName: String) = EdziennikTask(profileId, AttachmentGetRequest(message, attachmentId, attachmentName))
        fun recipientListGet(profileId: Int) = EdziennikTask(profileId, RecipientListGetRequest())
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
    }

    private var edziennikInterface: EdziennikInterface? = null

    internal fun run(app: App, taskCallback: EdziennikCallback) {
        if (profile?.archived == true) {
            taskCallback.onError(ApiError(TAG, ERROR_PROFILE_ARCHIVED))
            return
        }
        edziennikInterface = when (loginStore.type) {
            LOGIN_TYPE_LIBRUS -> Librus(app, profile, loginStore, taskCallback)
            LOGIN_TYPE_MOBIDZIENNIK -> Mobidziennik(app, profile, loginStore, taskCallback)
            LOGIN_TYPE_VULCAN -> Vulcan(app, profile, loginStore, taskCallback)
            LOGIN_TYPE_IDZIENNIK -> Idziennik(app, profile, loginStore, taskCallback)
            LOGIN_TYPE_EDUDZIENNIK -> Edudziennik(app, profile, loginStore, taskCallback)
            LOGIN_TYPE_TEMPLATE -> Template(app, profile, loginStore, taskCallback)
            else -> null
        }
        if (edziennikInterface == null) {
            return
        }

        when (request) {
            is SyncProfileRequest -> edziennikInterface?.sync(
                    featureIds = request.viewIds?.flatMap { Features.getIdsByView(it.first, it.second) }
                            ?: Features.getAllIds(),
                    viewId = request.viewIds?.get(0)?.first,
                    onlyEndpoints = request.onlyEndpoints,
                    arguments = request.arguments)
            is MessageGetRequest -> edziennikInterface?.getMessage(request.message)
            is MessageSendRequest -> edziennikInterface?.sendMessage(request.recipients, request.subject, request.text)
            is FirstLoginRequest -> edziennikInterface?.firstLogin()
            is AnnouncementsReadRequest -> edziennikInterface?.markAllAnnouncementsAsRead()
            is AnnouncementGetRequest -> edziennikInterface?.getAnnouncement(request.announcement)
            is AttachmentGetRequest -> edziennikInterface?.getAttachment(request.message, request.attachmentId, request.attachmentName)
            is RecipientListGetRequest -> edziennikInterface?.getRecipientList()
        }
    }

    override fun cancel() {
        edziennikInterface?.cancel()
    }

    override fun toString(): String {
        return "EdziennikTask(profileId=$profileId, request=$request, edziennikInterface=$edziennikInterface)"
    }

    data class FirstLoginRequest(val loginStore: LoginStore)
    class SyncRequest
    data class SyncProfileRequest(val viewIds: List<Pair<Int, Int>>? = null, val onlyEndpoints: List<Int>? = null, val arguments: JsonObject? = null)
    data class SyncProfileListRequest(val profileList: List<Int>)
    data class MessageGetRequest(val message: MessageFull)
    data class MessageSendRequest(val recipients: List<Teacher>, val subject: String, val text: String)
    class AnnouncementsReadRequest
    data class AnnouncementGetRequest(val announcement: AnnouncementFull)
    data class AttachmentGetRequest(val message: Message, val attachmentId: Long, val attachmentName: String)
    class RecipientListGetRequest
}
