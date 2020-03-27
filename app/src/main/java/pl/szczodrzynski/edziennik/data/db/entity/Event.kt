/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import com.google.gson.annotations.SerializedName
import pl.szczodrzynski.edziennik.MINUTE
import pl.szczodrzynski.edziennik.data.db.full.EventFull
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
        var teamId: Long
) {
    companion object {
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
        const val TYPE_TEACHER_ABSENCE = 10L
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
        const val COLOR_TEACHER_ABSENCE = 0xff039be5.toInt()
    }

    @ColumnInfo(name = "eventAddedManually")
    var addedManually: Boolean = false
    @ColumnInfo(name = "eventSharedBy")
    var sharedBy: String? = null
    @ColumnInfo(name = "eventSharedByName")
    var sharedByName: String? = null
    @ColumnInfo(name = "eventBlacklisted")
    var blacklisted: Boolean = false

    var homeworkBody: String? = null
    var attachmentIds: List<Long>? = null
    var attachmentNames: List<String>? = null

    @Ignore
    var showAsUnseen = false

    val startTimeCalendar: Calendar
        get() = Calendar.getInstance().also { it.set(
                date.year,
                date.month - 1,
                date.day,
                time?.hour ?: 0,
                time?.minute ?: 0,
                time?.second ?: 0
        ) }

    val endTimeCalendar: Calendar
        get() = startTimeCalendar.also {
            it.timeInMillis += 45 * MINUTE * 1000
        }

    @Ignore
    fun withMetadata(metadata: Metadata) = EventFull(this, metadata)
}
