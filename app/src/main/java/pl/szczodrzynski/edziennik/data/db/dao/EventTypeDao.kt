/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.dao

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.config.AppData
import pl.szczodrzynski.edziennik.data.db.entity.EventType
import pl.szczodrzynski.edziennik.data.db.entity.EventType.Companion.SOURCE_DEFAULT
import pl.szczodrzynski.edziennik.data.db.entity.Profile

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

    fun addDefaultTypes(profile: Profile): List<EventType> {
        val data = AppData.get(profile.loginStoreType)
        var order = 100
        val typeList = data.eventTypes.map {
            EventType(
                profileId = profile.id,
                id = it.id.toLong(),
                name = it.name,
                color = Color.parseColor(it.color),
                order = order++,
                source = SOURCE_DEFAULT,
            )
        }
        addAll(typeList)
        return typeList
    }
}
