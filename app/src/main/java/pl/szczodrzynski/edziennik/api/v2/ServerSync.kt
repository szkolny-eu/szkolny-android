package pl.szczodrzynski.edziennik.api.v2

import pl.szczodrzynski.edziennik.App.APP_URL
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_AGENDA
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_HOMEWORK
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.api.v2.models.Data
import pl.szczodrzynski.edziennik.data.api.AppError.CODE_APP_SERVER_ERROR
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_HOMEWORK
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_SHARED_EVENT
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_SHARED_HOMEWORK
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_SERVER_MESSAGE
import pl.szczodrzynski.edziennik.data.db.modules.notification.getNotificationTitle
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.network.ServerRequest

class ServerSync(val data: Data, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "ServerSync"
    }

    val app = data.app
    val profileId = data.profile?.id ?: -1
    val profileName = data.profile?.name ?: ""
    val profile = data.profile
    val loginStore = data.loginStore

    private fun getUsernameId(): String {
        if (loginStore.data == null) {
            return "NO_LOGIN_STORE"
        }
        if (profile?.studentData == null) {
            return "NO_STUDENT_STORE"
        }
        return when (data.loginStore.type) {
            LoginStore.LOGIN_TYPE_MOBIDZIENNIK -> loginStore.getLoginData("serverName", "MOBI_UN") + ":" + loginStore.getLoginData("username", "MOBI_UN") + ":" + profile.getStudentData("studentId", -1)
            LoginStore.LOGIN_TYPE_LIBRUS -> profile.getStudentData("schoolName", "LIBRUS_UN") + ":" + profile.getStudentData("accountLogin", "LIBRUS_LOGIN_UN")
            LoginStore.LOGIN_TYPE_IUCZNIOWIE -> loginStore.getLoginData("schoolName", "IUCZNIOWIE_UN") + ":" + loginStore.getLoginData("username", "IUCZNIOWIE_UN") + ":" + profile.getStudentData("registerId", -1)
            LoginStore.LOGIN_TYPE_VULCAN -> profile.getStudentData("schoolName", "VULCAN_UN") + ":" + profile.getStudentData("studentId", -1)
            LoginStore.LOGIN_TYPE_DEMO -> loginStore.getLoginData("serverName", "DEMO_UN") + ":" + loginStore.getLoginData("username", "DEMO_UN") + ":" + profile.getStudentData("studentId", -1)
            else -> "TYPE_UNKNOWN"
        }
    }

    init { run {
        if (profile?.registration != Profile.REGISTRATION_ENABLED) {
            onSuccess()
            return@run
        }

        val request = ServerRequest(
                app,
                app.requestScheme+APP_URL+"main.php?sync",
                "Edziennik2/REG",
                profile,
                data.loginStore.type,
                getUsernameId()
        )

        if (profile.empty) {
            request.setBodyParameter("first_run", "true")
        }

        var hasNotifications = true
        if (app.appConfig.webPushEnabled) {
            data.notifications
                    .filterNot { it.posted }
                    .let {
                        if (it.isEmpty()) {
                            hasNotifications = false
                            null
                        }
                        else
                            it
                    }?.forEachIndexed { index, notification ->
                        if (notification.type != TYPE_NEW_SHARED_EVENT
                                && notification.type != TYPE_SERVER_MESSAGE
                                && notification.type != TYPE_NEW_SHARED_HOMEWORK) {
                            request.setBodyParameter("notify[$index][type]", notification.type.toString())
                            request.setBodyParameter("notify[$index][title]", notification.title)
                            request.setBodyParameter("notify[$index][text]", notification.text)
                        }
                    }
        }

        if ((!app.appConfig.webPushEnabled || !hasNotifications) && !profile.enableSharedEvents) {
            onSuccess()
            return@run
        }

        val result = request.runSync()

        if (result == null) {
            data.error(ApiError(TAG, CODE_APP_SERVER_ERROR)
                    .setCritical(false))
            onSuccess()
            return@run
        }
        var apiResponse = result.toString()
        if (result.getString("success") != "true") {
            data.error(ApiError(TAG, CODE_APP_SERVER_ERROR)
                    .setCritical(false))
            onSuccess()
            return@run
        }
        // HERE PROCESS ALL THE RECEIVED EVENTS
        // add them to the profile and create appropriate notifications
        result.getJsonArray("events")?.forEach { jEventEl ->
            val event = jEventEl.asJsonObject
            val teamCode = event.getString("team")

            // get the target Team from teamCode
            val team = app.db.teamDao().getByCodeNow(profile.id, teamCode)
            if (team != null) {

                // create the event from Json. Add the missing teamId and !!profileId!!
                val eventObject = app.gson.fromJson(event.toString(), Event::class.java)
                // proguard. disable for Event.class
                if (eventObject.eventDate == null) {
                    apiResponse += "\n\nEventDate == null\n$event"
                }
                eventObject.profileId = profileId
                eventObject.teamId = team.id
                eventObject.addedManually = true

                if (eventObject.sharedBy == getUsernameId()) {
                    eventObject.sharedBy = "self"
                    eventObject.sharedByName = profile.studentNameLong
                }

                val typeObject = app.db.eventTypeDao().getByIdNow(profileId, eventObject.type)

                app.db.eventDao().add(eventObject)

                val metadata = Metadata(
                        profileId,
                        if (eventObject.type == TYPE_HOMEWORK) Metadata.TYPE_HOMEWORK else Metadata.TYPE_EVENT,
                        eventObject.id,
                        profile.empty,
                        true,
                        event.getLong("addedDate") ?: 0
                )

                val metadataId = app.db.metadataDao().add(metadata)

                // notify if the event is new and not first sync
                if (metadataId != -1L && !profile.empty) {
                    val text = app.getString(
                            R.string.notification_shared_event_format,
                            eventObject.sharedByName,
                            if (typeObject != null) typeObject.name else "wydarzenie",
                            if (eventObject.eventDate == null) "???" else eventObject.eventDate.formattedString,
                            eventObject.topic
                    )
                    val type = if (eventObject.type == TYPE_HOMEWORK) TYPE_NEW_SHARED_HOMEWORK else TYPE_NEW_SHARED_EVENT
                    data.notifications += Notification(
                            title = app.getNotificationTitle(type),
                            text = text,
                            type = type,
                            profileId = profileId,
                            profileName = profileName,
                            viewId = if (eventObject.type == TYPE_HOMEWORK) DRAWER_ITEM_HOMEWORK else DRAWER_ITEM_AGENDA,
                            addedDate = metadata.addedDate
                    ).addExtra("eventId", eventObject.id).addExtra("eventDate", eventObject.eventDate.value.toLong())
                }
            }
        }

        onSuccess()
    }}
}