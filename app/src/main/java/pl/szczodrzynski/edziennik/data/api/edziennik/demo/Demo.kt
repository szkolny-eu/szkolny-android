/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-8.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.demo

import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull

class Demo(
    val app: App,
    val profile: Profile?,
    val loginStore: LoginStore,
    val callback: EdziennikCallback,
) : EdziennikInterface {

    private fun completed() {
        callback.onCompleted()
    }

    override fun sync(
        featureTypes: Set<FeatureType>?,
        onlyEndpoints: Set<Int>?,
        arguments: JsonObject?,
    ) = completed()

    override fun getMessage(message: MessageFull) =
        completed()

    override fun sendMessage(recipients: Set<Teacher>, subject: String, text: String) =
        completed()

    override fun markAllAnnouncementsAsRead() =
        completed()

    override fun getAnnouncement(announcement: AnnouncementFull) =
        completed()

    override fun getAttachment(owner: Any, attachmentId: Long, attachmentName: String) =
        completed()

    override fun getRecipientList() =
        completed()

    override fun getEvent(eventFull: EventFull) =
        completed()

    override fun firstLogin() {
        val profile = Profile(
            id = loginStore.id,
            loginStoreId = loginStore.id,
            loginStoreType = LoginType.DEMO,
            name = "Jan Szkolny",
            subname = "Szkolny.eu",
            studentNameLong = "Jan Szkolny",
            studentNameShort = "Jan S.",
            accountName = null,
        )
        profile.apply {
            empty = false
            syncEnabled = false
            registration = Profile.REGISTRATION_DISABLED
            studentClassName = "1A"
            userCode = "nologin:1234"
            dateYearEnd.month = 8
        }
        EventBus.getDefault().postSticky(FirstLoginFinishedEvent(listOf(profile), loginStore))
        completed()
    }

    override fun cancel() {}
}
