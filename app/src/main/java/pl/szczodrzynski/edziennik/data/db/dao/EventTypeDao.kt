/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.dao

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_CLASS_EVENT
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_DEFAULT
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
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_ESSAY
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_EXAM
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_EXCURSION
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_HOMEWORK
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_INFORMATION
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_PROJECT
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_PT_MEETING
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_READING
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_SHORT_QUIZ
import pl.szczodrzynski.edziennik.data.db.entity.EventType

@Dao
abstract class EventTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun add(eventType: EventType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addAll(eventTypeList: List<EventType>)

    @Query("DELETE FROM eventTypes WHERE profileId = :profileId")
    abstract fun clear(profileId: Int)

    @Query("SELECT * FROM eventTypes WHERE profileId = :profileId AND eventType = :typeId")
    abstract fun getByIdNow(profileId: Int, typeId: Long): EventType?

    @Query("SELECT * FROM eventTypes WHERE profileId = :profileId")
    abstract fun getAll(profileId: Int): LiveData<List<EventType>>

    @Query("SELECT * FROM eventTypes WHERE profileId = :profileId")
    abstract fun getAllNow(profileId: Int): List<EventType>

    @get:Query("SELECT * FROM eventTypes")
    abstract val allNow: List<EventType>

    fun addDefaultTypes(context: Context, profileId: Int): List<EventType> {
        val typeList = listOf(
                EventType(profileId, TYPE_HOMEWORK, context.getString(R.string.event_type_homework), COLOR_HOMEWORK),
                EventType(profileId, TYPE_DEFAULT, context.getString(R.string.event_other), COLOR_DEFAULT),
                EventType(profileId, TYPE_EXAM, context.getString(R.string.event_exam), COLOR_EXAM),
                EventType(profileId, TYPE_SHORT_QUIZ, context.getString(R.string.event_short_quiz), COLOR_SHORT_QUIZ),
                EventType(profileId, TYPE_ESSAY, context.getString(R.string.event_essay), COLOR_ESSAY),
                EventType(profileId, TYPE_PROJECT, context.getString(R.string.event_project), COLOR_PROJECT),
                EventType(profileId, TYPE_PT_MEETING, context.getString(R.string.event_pt_meeting), COLOR_PT_MEETING),
                EventType(profileId, TYPE_EXCURSION, context.getString(R.string.event_excursion), COLOR_EXCURSION),
                EventType(profileId, TYPE_READING, context.getString(R.string.event_reading), COLOR_READING),
                EventType(profileId, TYPE_CLASS_EVENT, context.getString(R.string.event_class_event), COLOR_CLASS_EVENT),
                EventType(profileId, TYPE_INFORMATION, context.getString(R.string.event_information), COLOR_INFORMATION)
        )
        addAll(typeList)
        return typeList
    }
}
