/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

@Dao
abstract class EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun add(event: Event): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addAll(eventList: List<Event>): LongArray

    @Query("DELETE FROM events WHERE profileId = :profileId")
    abstract fun clear(profileId: Int)

    @Query("DELETE FROM events WHERE profileId = :profileId AND eventId = :id")
    abstract fun remove(profileId: Int, id: Long)

    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = :thingType AND thingId = :thingId")
    abstract fun removeMetadata(profileId: Int, thingType: Int, thingId: Long)

    @Transaction
    open fun remove(profileId: Int, type: Long, id: Long) {
        remove(profileId, id)
        removeMetadata(profileId, if (type == Event.TYPE_HOMEWORK) Metadata.TYPE_HOMEWORK else Metadata.TYPE_EVENT, id)
    }

    @Transaction
    open fun remove(event: Event) {
        remove(event.profileId, event.type, event.id)
    }

    @Transaction
    open fun remove(profileId: Int, event: Event) {
        remove(profileId, event.type, event.id)
    }

    @Query("DELETE FROM events WHERE teamId = :teamId AND eventId = :id")
    abstract fun removeByTeamId(teamId: Long, id: Long)

    @RawQuery(observedEntities = [Event::class])
    abstract fun getAll(query: SupportSQLiteQuery): LiveData<List<EventFull>>

    fun getAll(profileId: Int, filter: String, limit: String): LiveData<List<EventFull>> {
        return getAll(SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName,\n" +
                "eventTypes.eventTypeName AS typeName,\n" +
                "eventTypes.eventTypeColor AS typeColor\n" +
                "FROM events\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "LEFT JOIN eventTypes USING(profileId, eventType)\n" +
                "LEFT JOIN metadata ON eventId = thingId AND (thingType = " + Metadata.TYPE_EVENT + " OR thingType = " + Metadata.TYPE_HOMEWORK + ") AND metadata.profileId = " + profileId + "\n" +
                "WHERE events.profileId = " + profileId + " AND events.eventBlacklisted = 0 AND " + filter + "\n" +
                "GROUP BY eventId\n" +
                "ORDER BY eventDate, eventTime ASC " + limit))
    }

    fun getAll(profileId: Int): LiveData<List<EventFull>> {
        return getAll(profileId, "1", "")
    }

    fun getAllNow(profileId: Int): List<EventFull> {
        return getAllNow(profileId, "1")
    }

    fun getAllWhere(profileId: Int, filter: String): LiveData<List<EventFull>> {
        return getAll(profileId, filter, "")
    }

    fun getAllByType(profileId: Int, type: Long, filter: String): LiveData<List<EventFull>> {
        return getAll(profileId, "eventType = $type AND $filter", "")
    }

    fun getAllByDate(profileId: Int, date: Date): LiveData<List<EventFull>> {
        return getAll(profileId, "eventDate = '" + date.stringY_m_d + "'", "")
    }

    fun getAllByDateNow(profileId: Int, date: Date): List<EventFull> {
        return getAllNow(profileId, "eventDate = '" + date.stringY_m_d + "'")
    }

    fun getAllByDateTime(profileId: Int, date: Date, time: Time?): LiveData<List<EventFull>> {
        return if (time == null) getAllByDate(profileId, date) else getAll(profileId, "eventDate = '" + date.stringY_m_d + "' AND eventTime = '" + time.stringValue + "'", "")
    }

    fun getAllNearest(profileId: Int, today: Date, limit: Int): LiveData<List<EventFull>> {
        return getAll(profileId, "eventDate >= '" + today.stringY_m_d + "'", "LIMIT $limit")
    }

    @RawQuery
    abstract fun getAllNow(query: SupportSQLiteQuery): List<EventFull>

    fun getAllNow(profileId: Int, filter: String): List<EventFull> {
        return getAllNow(SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName,\n" +
                "eventTypes.eventTypeName AS typeName,\n" +
                "eventTypes.eventTypeColor AS typeColor\n" +
                "FROM events \n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "LEFT JOIN eventTypes USING(profileId, eventType)\n" +
                "LEFT JOIN metadata ON eventId = thingId AND (thingType = " + Metadata.TYPE_EVENT + " OR thingType = " + Metadata.TYPE_HOMEWORK + ") AND metadata.profileId = " + profileId + "\n" +
                "WHERE events.profileId = " + profileId + " AND events.eventBlacklisted = 0 AND " + filter + "\n" +
                "GROUP BY eventId\n" +
                "ORDER BY eventTime, addedDate ASC"))
    }

    fun getNotNotifiedNow(profileId: Int): List<EventFull> {
        return getAllNow(profileId, "notified = 0")
    }

    @Query("SELECT eventId FROM events WHERE profileId = :profileId AND eventBlacklisted = 1")
    abstract fun getBlacklistedIds(profileId: Int): List<Long>

    @get:Query("SELECT eventId FROM events WHERE eventBlacklisted = 1")
    abstract val blacklistedIds: List<Long>

    @get:Query("SELECT " +
            "*, " +
            "eventTypes.eventTypeName AS typeName, " +
            "eventTypes.eventTypeColor AS typeColor " +
            "FROM events " +
            "LEFT JOIN subjects USING(profileId, subjectId) " +
            "LEFT JOIN eventTypes USING(profileId, eventType) " +
            "LEFT JOIN metadata ON eventId = thingId AND (thingType = " + Metadata.TYPE_EVENT + " OR thingType = " + Metadata.TYPE_HOMEWORK + ") AND metadata.profileId = events.profileId " +
            "WHERE events.eventBlacklisted = 0 AND notified = 0 " +
            "GROUP BY eventId " +
            "ORDER BY addedDate ASC")
    abstract val notNotifiedNow: List<EventFull>

    fun getByIdNow(profileId: Int, eventId: Long): EventFull? {
        val eventList = getAllNow(profileId, "eventId = $eventId")
        return if (eventList.isEmpty()) null else eventList[0]
    }

    @Query("UPDATE events SET eventAddedManually = 1 WHERE profileId = :profileId AND eventDate < :date")
    abstract fun convertOlderToManual(profileId: Int, date: Date?)

    @Query("DELETE FROM events WHERE profileId = :profileId AND eventAddedManually = 0")
    abstract fun removeNotManual(profileId: Int)

    @RawQuery
    abstract fun removeFuture(query: SupportSQLiteQuery?): Long

    @Transaction
    open fun removeFuture(profileId: Int, todayDate: Date, filter: String) {
        removeFuture(SimpleSQLiteQuery("DELETE FROM events WHERE profileId = " + profileId
                + " AND eventAddedManually = 0 AND eventDate >= '" + todayDate.stringY_m_d + "'" +
                " AND " + filter))
    }

    @Query("DELETE FROM events WHERE profileId = :profileId AND eventAddedManually = 0 AND eventDate >= :todayDate AND eventType = :type")
    abstract fun removeFutureWithType(profileId: Int, todayDate: Date, type: Long)

    @Query("DELETE FROM events WHERE profileId = :profileId AND eventAddedManually = 0 AND eventDate >= :todayDate AND eventType != :exceptType")
    abstract fun removeFutureExceptType(profileId: Int, todayDate: Date, exceptType: Long)

    @Transaction
    open fun removeFutureExceptTypes(profileId: Int, todayDate: Date, exceptTypes: List<Long>) {
        removeFuture(profileId, todayDate, "eventType NOT IN " + exceptTypes.toString().replace('[', '(').replace(']', ')'))
    }

    @Query("UPDATE metadata SET seen = :seen WHERE profileId = :profileId AND (thingType = " + Metadata.TYPE_EVENT + " OR thingType = " + Metadata.TYPE_LESSON_CHANGE + " OR thingType = " + Metadata.TYPE_HOMEWORK + ") AND thingId IN (SELECT eventId FROM events WHERE profileId = :profileId AND eventDate = :date)")
    abstract fun setSeenByDate(profileId: Int, date: Date, seen: Boolean)

    @Query("UPDATE events SET eventBlacklisted = :blacklisted WHERE profileId = :profileId AND eventId = :eventId")
    abstract fun setBlacklisted(profileId: Int, eventId: Long, blacklisted: Boolean)
}
