/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient;
import pl.szczodrzynski.edziennik.data.db.full.MessageRecipientFull;

@Dao
public abstract class MessageRecipientDao {
    @Query("DELETE FROM messageRecipients WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long add(MessageRecipient messageRecipient);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addAll(List<MessageRecipient> messageRecipientList);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void addAllIgnore(List<MessageRecipient> messageRecipientList);

    @RawQuery(observedEntities = {MessageRecipient.class})
    abstract List<MessageRecipientFull> getNow(SupportSQLiteQuery query);

    public List<MessageRecipientFull> getAll(int profileId) {
        return getNow(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS fullName\n" +
                "FROM messageRecipients \n" +
                "LEFT JOIN teachers ON teachers.profileId = "+profileId+" AND teacherId = messageRecipients.messageRecipientId\n" +
                "WHERE messageRecipients.profileId = "+profileId+"\n"));
    }

    public List<MessageRecipientFull> getAllByMessageId(int profileId, long messageId) {
        return getNow(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS fullName\n" +
                "FROM messageRecipients \n" +
                "LEFT JOIN teachers ON teachers.profileId = "+profileId+" AND teacherId = messageRecipients.messageRecipientId\n" +
                "WHERE messageRecipients.profileId = "+profileId+" AND messageRecipients.messageId = "+messageId+"\n"));
    }
}
