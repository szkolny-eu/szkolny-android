package pl.szczodrzynski.edziennik.api.v2.events.task

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.idziennik.Idziennik
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.api.v2.librus.Librus
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.Mobidziennik
import pl.szczodrzynski.edziennik.api.v2.template.Template
import pl.szczodrzynski.edziennik.api.v2.vulcan.Vulcan
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull

open class EdziennikTask(override val profileId: Int, val request: Any) : IApiTask(profileId) {
    companion object {
        private const val TAG = "EdziennikTask"

        fun firstLogin(loginStore: LoginStore) = EdziennikTask(-1, FirstLoginRequest(loginStore))
        fun sync() = EdziennikTask(-1, SyncRequest())
        fun syncProfile(profileId: Int, viewIds: List<Pair<Int, Int>>? = null, arguments: JsonObject? = null) = EdziennikTask(profileId, SyncProfileRequest(viewIds, arguments))
        fun syncProfileList(profileList: List<Int>) = EdziennikTask(-1, SyncProfileListRequest(profileList))
        fun messageGet(profileId: Int, message: MessageFull) = EdziennikTask(profileId, MessageGetRequest(message))
        fun announcementsRead(profileId: Int) = EdziennikTask(profileId, AnnouncementsReadRequest())
        fun attachmentGet(profileId: Int, message: Message, attachmentId: Long, attachmentName: String) = EdziennikTask(profileId, AttachmentGetRequest(message, attachmentId, attachmentName))
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
            val profile = app.db.profileDao().getFullByIdNow(profileId)
            this.profile = profile
            if (profile == null) {
                return
            }
            val loginStore = app.db.loginStoreDao().getByIdNow(profile.loginStoreId) ?: return
            this.loginStore = loginStore
            // save the profile ID and name as the current task's
            taskName = app.getString(R.string.edziennik_notification_api_sync_title_format, profile.name)
        }
    }

    private var edziennikInterface: EdziennikInterface? = null

    internal fun run(app: App, taskCallback: EdziennikCallback) {
        edziennikInterface = when (loginStore.type) {
            LOGIN_TYPE_LIBRUS -> Librus(app, profile, loginStore, taskCallback)
            LOGIN_TYPE_MOBIDZIENNIK -> Mobidziennik(app, profile, loginStore, taskCallback)
            LOGIN_TYPE_VULCAN -> Vulcan(app, profile, loginStore, taskCallback)
            LOGIN_TYPE_IDZIENNIK -> Idziennik(app, profile, loginStore, taskCallback)
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
                    arguments = request.arguments)
            is MessageGetRequest -> edziennikInterface?.getMessage(request.message)
            is FirstLoginRequest -> edziennikInterface?.firstLogin()
            is AnnouncementsReadRequest -> edziennikInterface?.markAllAnnouncementsAsRead()
            is AttachmentGetRequest -> edziennikInterface?.getAttachment(request.message, request.attachmentId, request.attachmentName)
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
    data class SyncProfileRequest(val viewIds: List<Pair<Int, Int>>? = null, val arguments: JsonObject? = null)
    data class SyncProfileListRequest(val profileList: List<Int>)
    data class MessageGetRequest(val message: MessageFull)
    class AnnouncementsReadRequest
    data class AttachmentGetRequest(val message: Message, val attachmentId: Long, val attachmentName: String)
}
