/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.dao;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import pl.szczodrzynski.edziennik.data.db.entity.Event;
import pl.szczodrzynski.edziennik.data.db.full.EventFull;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

import static pl.szczodrzynski.edziennik.data.db.entity.Metadata.TYPE_EVENT;
import static pl.szczodrzynski.edziennik.data.db.entity.Metadata.TYPE_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.entity.Metadata.TYPE_LESSON_CHANGE;

@Dao
public abstract class EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long add(Event event);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long[] addAll(List<Event> eventList);

    @Query("DELETE FROM events WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    @Query("DELETE FROM events WHERE profileId = :profileId AND eventId = :id")
    public abstract void remove(int profileId, long id);
    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = :thingType AND thingId = :thingId")
    public abstract void removeMetadata(int profileId, int thingType, long thingId);
    @Transaction
    public void remove(int profileId, long type, long id) {
        remove(profileId, id);
        removeMetadata(profileId, type == Event.TYPE_HOMEWORK ? TYPE_HOMEWORK : TYPE_EVENT, id);
    }
    @Transaction
    public void remove(Event event) {
        remove(event.profileId, event.type, event.id);
    }
    @Transaction
    public void remove(int profileId, Event event) {
        remove(profileId, event.type, event.id);
    }
    @Query("DELETE FROM events WHERE teamId = :teamId AND eventId = :id")
    public abstract void removeByTeamId(long teamId, long id);

    @RawQuery(observedEntities = {Event.class})
    abstract LiveData<List<EventFull>> getAll(SupportSQLiteQuery query);
    public LiveData<List<EventFull>> getAll(int profileId, String filter) {
        String query = "SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName,\n" +
                "eventTypes.eventTypeName AS typeName,\n" +
                "eventTypes.eventTypeColor AS typeColor\n" +
                "FROM events\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "LEFT JOIN eventTypes USING(profileId, eventType)\n" +
                "LEFT JOIN metadata ON eventId = thingId AND (thingType = " + TYPE_EVENT + " OR thingType = " + TYPE_HOMEWORK + ") AND metadata.profileId = "+profileId+"\n" +
                "WHERE events.profileId = "+profileId+" AND events.eventBlacklisted = 0 AND "+filter+"\n" +
                "GROUP BY eventId\n" +
                "ORDER BY eventDate, eventStartTime ASC";
        Log.d("DB", query);
        return getAll(new SimpleSQLiteQuery(query));
    }
    public LiveData<List<EventFull>> getAll(int profileId) {
        return getAll(profileId, "1");
    }
    public List<EventFull> getAllNow(int profileId) {
        return getAllNow(profileId, "1");
    }
    public LiveData<List<EventFull>> getAllWhere(int profileId, String filter) {
        return getAll(profileId, filter);
    }
    public LiveData<List<EventFull>> getAllByType(int profileId, long type, String filter) {
        return getAll(profileId, "eventType = "+type+" AND "+filter);
    }
    public LiveData<List<EventFull>> getAllByDate(int profileId, @NonNull Date date) {
        return getAll(profileId, "eventDate = '"+date.getStringY_m_d()+"'");
    }
    public List<EventFull> getAllByDateNow(int profileId, @NonNull Date date) {
        return getAllNow(profileId, "eventDate = '"+date.getStringY_m_d()+"'");
    }
    public LiveData<List<EventFull>> getAllByDateTime(int profileId, @NonNull Date date, Time time) {
        if (time == null)
            return getAllByDate(profileId, date);
        return getAll(profileId, "eventDate = '"+date.getStringY_m_d()+"' AND eventStartTime = '"+time.getStringValue()+"'");
    }

    @RawQuery
    abstract List<EventFull> getAllNow(SupportSQLiteQuery query);
    public List<EventFull> getAllNow(int profileId, String filter) {
        return getAllNow(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName,\n" +
                "eventTypes.eventTypeName AS typeName,\n" +
                "eventTypes.eventTypeColor AS typeColor\n" +
                "FROM events \n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "LEFT JOIN eventTypes USING(profileId, eventType)\n" +
                "LEFT JOIN metadata ON eventId = thingId AND (thingType = " + TYPE_EVENT + " OR thingType = " + TYPE_HOMEWORK + ") AND metadata.profileId = "+profileId+"\n" +
                "WHERE events.profileId = "+profileId+" AND events.eventBlacklisted = 0 AND "+filter+"\n" +
                "GROUP BY eventId\n" +
                "ORDER BY eventStartTime, addedDate ASC"));
    }
    public List<EventFull> getNotNotifiedNow(int profileId) {
        return getAllNow(profileId, "notified = 0");
    }

    @Query("SELECT eventId FROM events WHERE profileId = :profileId AND eventBlacklisted = 1")
    public abstract List<Long> getBlacklistedIds(int profileId);
    @Query("SELECT eventId FROM events WHERE eventBlacklisted = 1")
    public abstract List<Long> getBlacklistedIds();

    @Query("SELECT " +
            "*, " +
            "eventTypes.eventTypeName AS typeName, " +
            "eventTypes.eventTypeColor AS typeColor " +
            "FROM events " +
            "LEFT JOIN subjects USING(profileId, subjectId) " +
            "LEFT JOIN eventTypes USING(profileId, eventType) " +
            "LEFT JOIN metadata ON eventId = thingId AND (thingType = " + TYPE_EVENT + " OR thingType = " + TYPE_HOMEWORK + ") AND metadata.profileId = events.profileId " +
            "WHERE events.eventBlacklisted = 0 AND notified = 0 " +
            "GROUP BY eventId " +
            "ORDER BY addedDate ASC")
    public abstract List<EventFull> getNotNotifiedNow();

    public EventFull getByIdNow(int profileId, long eventId) {
        List<EventFull> eventList = getAllNow(profileId, "eventId = "+eventId);
        return eventList.size() == 0 ? null : eventList.get(0);
    }

    @Query("UPDATE events SET eventAddedManually = 1 WHERE profileId = :profileId AND eventDate < :date")
    public abstract void convertOlderToManual(int profileId, Date date);

    @Query("DELETE FROM events WHERE profileId = :profileId AND eventAddedManually = 0")
    public abstract void removeNotManual(int profileId);

    @RawQuery
    abstract long removeFuture(SupportSQLiteQuery query);
    @Transaction
    public void removeFuture(int profileId, Date todayDate, String filter) {
        removeFuture(new SimpleSQLiteQuery("DELETE FROM events WHERE profileId = " + profileId
                + " AND eventAddedManually = 0 AND eventDate >= '" + todayDate.getStringY_m_d() + "'" +
                " AND " + filter));
    }

    @Query("DELETE FROM events WHERE profileId = :profileId AND eventAddedManually = 0 AND eventDate >= :todayDate AND eventType = :type")
    public abstract void removeFutureWithType(int profileId, Date todayDate, long type);

    @Query("DELETE FROM events WHERE profileId = :profileId AND eventAddedManually = 0 AND eventDate >= :todayDate AND eventType != :exceptType")
    public abstract void removeFutureExceptType(int profileId, Date todayDate, long exceptType);

    @Transaction
    public void removeFutureExceptTypes(int profileId, Date todayDate, List<Long> exceptTypes) {
        removeFuture(profileId, todayDate, "eventType NOT IN " + exceptTypes.toString().replace('[', '(').replace(']', ')'));
    }

    @Query("UPDATE metadata SET seen = :seen WHERE profileId = :profileId AND (thingType = "+TYPE_EVENT+" OR thingType = "+TYPE_LESSON_CHANGE+" OR thingType = "+TYPE_HOMEWORK+") AND thingId IN (SELECT eventId FROM events WHERE profileId = :profileId AND eventDate = :date)")
    public abstract void setSeenByDate(int profileId, Date date, boolean seen);

    @Query("UPDATE events SET eventBlacklisted = :blacklisted WHERE profileId = :profileId AND eventId = :eventId")
    public abstract void setBlacklisted(int profileId, long eventId, boolean blacklisted);
}

