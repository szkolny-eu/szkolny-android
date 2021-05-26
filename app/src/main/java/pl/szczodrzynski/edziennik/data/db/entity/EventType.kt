/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-19.
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_CLASS_EVENT
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_DEFAULT
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_ELEARNING
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_ESSAY
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_EXAM
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_EXCURSION
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_HOMEWORK
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_INFORMATION
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_PROJECT
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_PT_MEETING
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_READING
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_SHORT_QUIZ
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_CLASS_EVENT
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_DEFAULT
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_ELEARNING
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_ESSAY
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_EXAM
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_EXCURSION
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_HOMEWORK
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_INFORMATION
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_PROJECT
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_PT_MEETING
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_READING
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_SHORT_QUIZ

@Entity(
    tableName = "eventTypes",
    primaryKeys = ["profileId", "eventType"]
)
class EventType(
    val profileId: Int,

    @ColumnInfo(name = "eventType")
    val id: Long,

    @ColumnInfo(name = "eventTypeName")
    val name: String,
    @ColumnInfo(name = "eventTypeColor")
    val color: Int,
    @ColumnInfo(name = "eventTypeOrder")
    var order: Int = id.toInt(),
    @ColumnInfo(name = "eventTypeSource")
    val source: Int = SOURCE_REGISTER
) {
    companion object {
        const val SOURCE_DEFAULT = 0
        const val SOURCE_REGISTER = 1
        const val SOURCE_CUSTOM = 2
        const val SOURCE_SHARED = 3

        fun getTypeColorMap() = mapOf(
            TYPE_ELEARNING to COLOR_ELEARNING,
            TYPE_HOMEWORK to COLOR_HOMEWORK,
            TYPE_DEFAULT to COLOR_DEFAULT,
            TYPE_EXAM to COLOR_EXAM,
            TYPE_SHORT_QUIZ to COLOR_SHORT_QUIZ,
            TYPE_ESSAY to COLOR_ESSAY,
            TYPE_PROJECT to COLOR_PROJECT,
            TYPE_PT_MEETING to COLOR_PT_MEETING,
            TYPE_EXCURSION to COLOR_EXCURSION,
            TYPE_READING to COLOR_READING,
            TYPE_CLASS_EVENT to COLOR_CLASS_EVENT,
            TYPE_INFORMATION to COLOR_INFORMATION
        )

        fun getTypeNameMap() = mapOf(
            TYPE_ELEARNING to R.string.event_type_elearning,
            TYPE_HOMEWORK to R.string.event_type_homework,
            TYPE_DEFAULT to R.string.event_other,
            TYPE_EXAM to R.string.event_exam,
            TYPE_SHORT_QUIZ to R.string.event_short_quiz,
            TYPE_ESSAY to R.string.event_essay,
            TYPE_PROJECT to R.string.event_project,
            TYPE_PT_MEETING to R.string.event_pt_meeting,
            TYPE_EXCURSION to R.string.event_excursion,
            TYPE_READING to R.string.event_reading,
            TYPE_CLASS_EVENT to R.string.event_class_event,
            TYPE_INFORMATION to R.string.event_information
        )
    }
}
