/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.events.FeedbackMessageEvent
import pl.szczodrzynski.edziennik.data.api.events.RegisterAvailabilityEvent
import pl.szczodrzynski.edziennik.data.api.szkolny.response.RegisterAvailabilityStatus
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.data.api.task.PostNotifications
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ext.getLong
import pl.szczodrzynski.edziennik.ext.getNotificationTitle
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.sync.UpdateWorker
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
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
                "sharedNote" -> sharedNote(
                    message.data.getString("shareTeamCode") ?: return@run,
                    message.data.getString("note") ?: return@run,
                    message.data.getString("message") ?: return@run,
                )
                "unsharedNote" -> unsharedNote(
                    message.data.getString("unshareTeamCode") ?: return@run,
                    message.data.getLong("noteId") ?: return@run,
                )
                "serverMessage",
                "unpairedBrowser" -> serverMessage(
                        message.data.getString("title") ?: "",
                        message.data.getString("message") ?: ""
                )
                "appUpdate" -> launch { UpdateWorker.runNow(app, app.gson.fromJson(message.data.getString("update"), Update::class.java)) }
                "feedbackMessage" -> launch {
                    val message = app.gson.fromJson(message.data.getString("message"), FeedbackMessage::class.java) ?: return@launch
                    feedbackMessage(message)
                }
                "registerAvailability" -> launch {
                    val data = app.gson.fromJson<Map<String, RegisterAvailabilityStatus>>(
                            message.data.getString("registerAvailability"),
                            object: TypeToken<Map<String, RegisterAvailabilityStatus>>(){}.type
                    ) ?: return@launch
                    app.config.sync.registerAvailability = data
                    if (EventBus.getDefault().hasSubscriberForEvent(RegisterAvailabilityEvent::class.java)) {
                        EventBus.getDefault().postSticky(RegisterAvailabilityEvent())
                    }
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

    private suspend fun feedbackMessage(message: FeedbackMessage) {
        if (message.deviceId == app.deviceId) {
            message.deviceId = null
            message.deviceName = null
        }
        withContext(Dispatchers.Default) {
            app.db.feedbackMessageDao().add(message)
            if (!EventBus.getDefault().hasSubscriberForEvent(FeedbackMessageEvent::class.java)) {
                val notification = Notification(
                        id = System.currentTimeMillis(),
                        title = "Wiadomość od ${message.senderName}",
                        text = message.text,
                        type = Notification.TYPE_FEEDBACK_MESSAGE,
                        profileId = null,
                        profileName = "Wiadomość od ${message.senderName}"
                ).addExtra("action", "feedbackMessage").addExtra("feedbackMessageDeviceId", message.deviceId)
                app.db.notificationDao().add(notification)
                PostNotifications(app, listOf(notification))
            }
            EventBus.getDefault().postSticky(FeedbackMessageEvent(message))
        }
    }

    private fun sharedEvent(teamCode: String, jsonStr: String, message: String) {
        val json = JsonParser.parseString(jsonStr).asJsonObject
        val teams = app.db.teamDao().allNow
        // not used, as the server provides a sharing message
        //val eventTypes = app.db.eventTypeDao().allNow

        val events = mutableListOf<Event>()
        val metadataList = mutableListOf<Metadata>()
        val notificationList = mutableListOf<Notification>()

        teams.filter { it.code == teamCode }.distinctBy { it.profileId }.forEach { team ->
            val profile = profiles.firstOrNull { it.id == team.profileId } ?: return@forEach
            if (!profile.canShare)
                return@forEach
            val event = Event(
                    profileId = team.profileId,
                    id = json.getLong("id") ?: return,
                    date = json.getInt("eventDate")?.let { Date.fromValue(it) } ?: return,
                    time = json.getInt("startTime")?.let { Time.fromValue(it) },
                    topic = json.getString("topicHtml") ?: json.getString("topic") ?: "",
                    color = json.getInt("color"),
                    type = json.getLong("type") ?: 0,
                    teacherId = json.getLong("teacherId") ?: -1,
                    subjectId = json.getLong("subjectId") ?: -1,
                    teamId = team.id,
                    addedDate = json.getLong("addedDate") ?: System.currentTimeMillis()
            )
            if (event.color == -1)
                event.color = null

            event.sharedBy = json.getString("sharedBy")
            event.sharedByName = json.getString("sharedByName")
            if (profile.userCode == event.sharedBy) {
                event.sharedBy = "self"
                event.addedManually = true
            }

            val metadata = Metadata(
                    event.profileId,
                    if (event.isHomework) MetadataType.HOMEWORK else MetadataType.EVENT,
                    event.id,
                    false,
                    true
            )

            val type = if (event.isHomework) Notification.TYPE_NEW_SHARED_HOMEWORK else Notification.TYPE_NEW_SHARED_EVENT
            val notificationFilter = app.config.getFor(event.profileId).sync.notificationFilter

            if (!notificationFilter.contains(type) && event.sharedBy != "self" && event.date >= Date.getToday()) {
                val notification = Notification(
                        id = Notification.buildId(event.profileId, type, event.id),
                        title = app.getNotificationTitle(type),
                        text = message,
                        type = type,
                        profileId = profile.id,
                        profileName = profile.name,
                        viewId = if (event.isHomework) NavTarget.HOMEWORK.id else NavTarget.AGENDA.id,
                        addedDate = event.addedDate
                ).addExtra("eventId", event.id).addExtra("eventDate", event.date.value.toLong())
                notificationList += notification
            }

            events += event
            metadataList += metadata
        }
        app.db.eventDao().upsertAll(events)
        app.db.metadataDao().addAllReplace(metadataList)
        if (notificationList.isNotEmpty()) {
            app.db.notificationDao().addAll(notificationList)
            PostNotifications(app, notificationList)
        }
    }

    private fun unsharedEvent(teamCode: String, eventId: Long, message: String) {
        val teams = app.db.teamDao().allNow
        val notificationList = mutableListOf<Notification>()

        teams.filter { it.code == teamCode }.distinctBy { it.profileId }.forEach { team ->
            val profile = profiles.firstOrNull { it.id == team.profileId } ?: return@forEach
            if (!profile.canShare)
                return@forEach
            val notificationFilter = app.config.getFor(team.profileId).sync.notificationFilter

            if (!notificationFilter.contains(Notification.TYPE_REMOVED_SHARED_EVENT)) {
                val notification = Notification(
                        id = Notification.buildId(profile.id, Notification.TYPE_REMOVED_SHARED_EVENT, eventId),
                        title = app.getNotificationTitle(Notification.TYPE_REMOVED_SHARED_EVENT),
                        text = message,
                        type = Notification.TYPE_REMOVED_SHARED_EVENT,
                        profileId = profile.id,
                        profileName = profile.name,
                        viewId = NavTarget.AGENDA.id
                )
                notificationList += notification
            }
            app.db.eventDao().remove(team.profileId, eventId)
        }
        if (notificationList.isNotEmpty()) {
            app.db.notificationDao().addAll(notificationList)
            PostNotifications(app, notificationList)
        }
    }

    private fun sharedNote(teamCode: String, jsonStr: String, message: String) {
        val note = app.gson.fromJson(jsonStr, Note::class.java)
        val noteSharedBy = note.sharedBy
        val teams = app.db.teamDao().allNow
        // not used, as the server provides a sharing message
        //val eventTypes = app.db.eventTypeDao().allNow

        val notes = mutableListOf<Note>()
        val notificationList = mutableListOf<Notification>()

        teams.filter { it.code == teamCode }.distinctBy { it.profileId }.forEach { team ->
            val profile = profiles.firstOrNull { it.id == team.profileId } ?: return@forEach
            if (!profile.canShare)
                return@forEach
            note.profileId = team.profileId
            if (profile.userCode == note.sharedBy) {
                note.sharedBy = "self"
            } else {
                note.sharedBy = noteSharedBy
            }

            if (!app.noteManager.hasValidOwner(note))
                return@forEach

            notes += note

            val hadNote = app.db.noteDao().getNow(note.profileId, note.id) != null
            // skip creating notifications
            if (hadNote)
                return@forEach

            val type = Notification.TYPE_NEW_SHARED_NOTE
            val notificationFilter = app.config.getFor(note.profileId).sync.notificationFilter

            if (!notificationFilter.contains(type) && note.sharedBy != "self") {
                val notification = Notification(
                    id = Notification.buildId(note.profileId, type, note.id),
                    title = app.getNotificationTitle(type),
                    text = message,
                    type = type,
                    profileId = profile.id,
                    profileName = profile.name,
                    viewId = NavTarget.HOME.id,
                    addedDate = note.addedDate
                ).addExtra("noteId", note.id)
                notificationList += notification
            }
        }
        app.db.noteDao().addAll(notes)
        if (notificationList.isNotEmpty()) {
            app.db.notificationDao().addAll(notificationList)
            PostNotifications(app, notificationList)
        }
    }

    private fun unsharedNote(teamCode: String, noteId: Long) {
        val teams = app.db.teamDao().allNow

        teams.filter { it.code == teamCode }.distinctBy { it.profileId }.forEach { team ->
            val profile = profiles.firstOrNull { it.id == team.profileId } ?: return@forEach
            if (!profile.canShare)
                return@forEach

            app.db.noteDao().remove(team.profileId, noteId)
        }
    }
}
