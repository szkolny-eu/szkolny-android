/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import com.google.gson.annotations.SerializedName
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.ext.MINUTE
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import java.util.*

@Entity(tableName = "events",
        primaryKeys = ["profileId", "eventId"],
        indices = [
            Index(value = ["profileId", "eventDate", "eventTime"]),
            Index(value = ["profileId", "eventType"])
        ])
open class Event(
        /* This needs to be mutable: see SzkolnyApi.getEvents() */
        var profileId: Int,
        @ColumnInfo(name = "eventId")
        var id: Long,
        @ColumnInfo(name = "eventDate")
        @SerializedName("eventDate")
        var date: Date,
        @ColumnInfo(name = "eventTime")
        @SerializedName("startTime")
        var time: Time?,

        @ColumnInfo(name = "eventTopic")
        var topic: String,
        @ColumnInfo(name = "eventColor")
        var color: Int?,
        @ColumnInfo(name = "eventType")
        var type: Long,

        var teacherId: Long,
        var subjectId: Long,
        var teamId: Long,
        var addedDate: Long = System.currentTimeMillis()
) : Keepable() {
    companion object {
        const val TYPE_ELEARNING = -5L
        const val TYPE_UNDEFINED = -2L
        const val TYPE_HOMEWORK = -1L
        const val TYPE_DEFAULT = 0L
        const val TYPE_EXAM = 1L
        const val TYPE_SHORT_QUIZ = 2L
        const val TYPE_ESSAY = 3L
        const val TYPE_PROJECT = 4L
        const val TYPE_PT_MEETING = 5L
        const val TYPE_EXCURSION = 6L
        const val TYPE_READING = 7L
        const val TYPE_CLASS_EVENT = 8L
        const val TYPE_INFORMATION = 9L
        const val COLOR_ELEARNING = 0xfff57f17.toInt()
        const val COLOR_HOMEWORK = 0xff795548.toInt()
        const val COLOR_DEFAULT = 0xffffc107.toInt()
        const val COLOR_EXAM = 0xfff44336.toInt()
        const val COLOR_SHORT_QUIZ = 0xff76ff03.toInt()
        const val COLOR_ESSAY = 0xFF4050B5.toInt()
        const val COLOR_PROJECT = 0xFF673AB7.toInt()
        const val COLOR_PT_MEETING = 0xff90caf9.toInt()
        const val COLOR_EXCURSION = 0xFF4CAF50.toInt()
        const val COLOR_READING = 0xFFFFEB3B.toInt()
        const val COLOR_CLASS_EVENT = 0xff388e3c.toInt()
        const val COLOR_INFORMATION = 0xff039be5.toInt()
    }

    /**
     * Added manually - added by self, shared by self, or shared by someone else.
     */
    @ColumnInfo(name = "eventAddedManually")
    var addedManually: Boolean = false
        get() = field || isShared

    /**
     * Shared by - user code who shared the event. Null if not shared.
     * "Self" if shared by this app user.
     */
    @ColumnInfo(name = "eventSharedBy")
    var sharedBy: String? = null
    @ColumnInfo(name = "eventSharedByName")
    var sharedByName: String? = null

    @ColumnInfo(name = "eventBlacklisted")
    var blacklisted: Boolean = false
    @ColumnInfo(name = "eventIsDone")
    var isDone: Boolean = false

    /**
     * Whether the full contents of the event are already stored locally.
     * There may be a need to download the full topic or body.
     */
    @ColumnInfo(name = "eventIsDownloaded")
    var isDownloaded: Boolean = true

    /**
     * Body/text of the event, if this is a [TYPE_HOMEWORK].
     * May be null if the body is not downloaded yet, or the type is not [TYPE_HOMEWORK].
     * May be empty or blank if the homework has no specific body attached,
     * or the topic contains the body already.
     */
    var homeworkBody: String? = null
    val hasAttachments
        get() = attachmentIds.isNotNullNorEmpty()
    var attachmentIds: MutableList<Long>? = null
    var attachmentNames: MutableList<String>? = null

    val isHomework
        get() = type == TYPE_HOMEWORK

    /**
     * Whether the event is shared by anyone. Note that this implies [addedManually].
     */
    val isShared
        get() = sharedBy != null

    /**
     * Whether the event is shared by "self" (this app user).
     */
    val isSharedSent
        get() = sharedBy == "self"

    /**
     * Whether the event is shared by someone else from the class group.
     */
    val isSharedReceived
        get() = sharedBy != null && sharedBy != "self"

    /**
     * Add an attachment
     * @param id attachment ID
     * @param name file name incl. extension
     * @return a Event to which the attachment has been added
     */
    fun addAttachment(id: Long, name: String): Event {
        if (attachmentIds == null) attachmentIds = mutableListOf()
        if (attachmentNames == null) attachmentNames = mutableListOf()
        attachmentIds?.add(id)
        attachmentNames?.add(name)
        return this
    }

    fun clearAttachments() {
        attachmentIds = null
        attachmentNames = null
    }

    @Ignore
    var showAsUnseen: Boolean? = null

    val startTimeCalendar: Calendar
        get() = date.getAsCalendar(time)

    val endTimeCalendar: Calendar
        get() = startTimeCalendar.also {
            it.timeInMillis += 45 * MINUTE * 1000
        }

    @Ignore
    fun withMetadata(metadata: Metadata) = EventFull(this, metadata)
}
