/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.events.FeedbackMessageEvent
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.data.api.task.PostNotifications
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.sync.UpdateWorker
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.coroutines.CoroutineContext

class SzkolnyAppFirebase(val app: App, val profiles: List<Profile>, val message: FirebaseService.Message) : CoroutineScope {
    companion object {
        private const val TAG = "SzkolnyAppFirebase"
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    init {
        run {
            val type = message.data.getString("type") ?: return@run
            when (type) {
                "sharedEvent" -> sharedEvent(
                        message.data.getString("shareTeamCode") ?: return@run,
                        message.data.getString("event") ?: return@run,
                        message.data.getString("message") ?: return@run
                )
                "unsharedEvent" -> unsharedEvent(
                        message.data.getString("unshareTeamCode") ?: return@run,
                        message.data.getLong("eventId") ?: return@run,
                        message.data.getString("message") ?: return@run
                )
                "serverMessage",
                "unpairedBrowser" -> serverMessage(
                        message.data.getString("title") ?: "",
                        message.data.getString("message") ?: ""
                )
                "appUpdate" -> launch { UpdateWorker.runNow(app, app.gson.fromJson(message.data.getString("update"), Update::class.java)) }
                "feedbackMessage" -> launch {
                    val message = app.gson.fromJson(message.data.getString("message"), FeedbackMessage::class.java)
                    if (message.deviceId == app.deviceId) {
                        message.deviceId = null
                        message.deviceName = null
                    }
                    withContext(Dispatchers.Default) {
                        app.db.feedbackMessageDao().add(message)
                    }
                    EventBus.getDefault().postSticky(FeedbackMessageEvent(message))
                }
            }
        }
    }

    private fun serverMessage(title: String, message: String) {
        val notification = Notification(
                id = System.currentTimeMillis(),
                title = title,
                text = message,
                type = Notification.TYPE_SERVER_MESSAGE,
                profileId = null,
                profileName = title
        ).addExtra("action", "serverMessage").addExtra("serverMessageTitle", title).addExtra("serverMessageText", message)
        app.db.notificationDao().add(notification)
        PostNotifications(app, listOf(notification))
    }

    private fun sharedEvent(teamCode: String, jsonStr: String, message: String) {
        val json = JsonParser().parse(jsonStr).asJsonObject
        val teams = app.db.teamDao().allNow
        // not used, as the server provides a sharing message
        //val eventTypes = app.db.eventTypeDao().allNow

        val events = mutableListOf<Event>()
        val metadataList = mutableListOf<Metadata>()
        val notificationList = mutableListOf<Notification>()

        teams.filter { it.code == teamCode }.distinctBy { it.profileId }.forEach { team ->
            val profile = profiles.firstOrNull { it.id == team.profileId }
            val event = Event(
                    team.profileId,
                    json.getLong("id") ?: return,
                    json.getInt("eventDate")?.let { Date.fromValue(it) } ?: return,
                    json.getInt("startTime")?.let { Time.fromValue(it) },
                    json.getString("topic") ?: "",
                    json.getInt("color") ?: -1,
                    json.getLong("type") ?: 0,
                    true,
                    json.getLong("teacherId") ?: -1,
                    json.getLong("subjectId") ?: -1,
                    team.id
            )

            // TODO? i guess - this comment is here for like a year
            //val oldEvent: Event? = app.db.eventDao().getByIdNow(profile?.id ?: -1, event.id)

            event.sharedBy = json.getString("sharedBy")
            event.sharedByName = json.getString("sharedByName")
            if (profile?.userCode == event.sharedBy) event.sharedBy = "self"

            val metadata = Metadata(
                    event.profileId,
                    if (event.type == Event.TYPE_HOMEWORK) Metadata.TYPE_HOMEWORK else Metadata.TYPE_EVENT,
                    event.id,
                    false,
                    true,
                    json.getLong("addedDate") ?: System.currentTimeMillis()
            )

            //val eventType = eventTypes.firstOrNull { it.profileId == profile?.id && it.id == event.type }

            /*val text = app.getString(
                    if (oldEvent == null)
                        R.string.notification_shared_event_format
                    else
                        R.string.notification_shared_event_modified_format,
                    event.sharedByName,
                    eventType?.name ?: "wydarzenie",
                    event.eventDate.formattedString,
                    event.topic
            )*/
            val type = if (event.type == Event.TYPE_HOMEWORK) Notification.TYPE_NEW_SHARED_HOMEWORK else Notification.TYPE_NEW_SHARED_EVENT
            val notification = Notification(
                    id = Notification.buildId(event.profileId, type, event.id),
                    title = app.getNotificationTitle(type),
                    text = message,
                    type = type,
                    profileId = profile?.id,
                    profileName = profile?.name,
                    viewId = if (event.type == Event.TYPE_HOMEWORK) MainActivity.DRAWER_ITEM_HOMEWORK else MainActivity.DRAWER_ITEM_AGENDA,
                    addedDate = metadata.addedDate
            ).addExtra("eventId", event.id).addExtra("eventDate", event.eventDate.value.toLong())

            events += event
            metadataList += metadata
            notificationList += notification
        }
        app.db.eventDao().addAll(events)
        app.db.metadataDao().addAllReplace(metadataList)
        app.db.notificationDao().addAll(notificationList)
        PostNotifications(app, notificationList)
    }

    private fun unsharedEvent(teamCode: String, eventId: Long, message: String) {
        val teams = app.db.teamDao().allNow
        val notificationList = mutableListOf<Notification>()

        teams.filter { it.code == teamCode }.distinctBy { it.profileId }.forEach { team ->
            val profile = profiles.firstOrNull { it.id == team.profileId }
            val notification = Notification(
                    id = Notification.buildId(profile?.id ?: 0, Notification.TYPE_REMOVED_SHARED_EVENT, eventId),
                    title = app.getNotificationTitle(Notification.TYPE_REMOVED_SHARED_EVENT),
                    text = message,
                    type = Notification.TYPE_REMOVED_SHARED_EVENT,
                    profileId = profile?.id,
                    profileName = profile?.name,
                    viewId = MainActivity.DRAWER_ITEM_AGENDA
            )
            notificationList += notification
            app.db.eventDao().remove(team.profileId, eventId)
        }
        app.db.notificationDao().addAll(notificationList)
        PostNotifications(app, notificationList)
    }
}
