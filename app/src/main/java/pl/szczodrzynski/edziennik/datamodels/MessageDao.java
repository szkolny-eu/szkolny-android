package pl.szczodrzynski.edziennik.datamodels;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import static pl.szczodrzynski.edziennik.datamodels.Message.TYPE_DELETED;
import static pl.szczodrzynski.edziennik.datamodels.Message.TYPE_RECEIVED;
import static pl.szczodrzynski.edziennik.datamodels.Message.TYPE_SENT;
import static pl.szczodrzynski.edziennik.datamodels.Metadata.TYPE_MESSAGE;

@Dao
public abstract class MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long add(Message message);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void addAllIgnore(List<Message> messageList);

    @Query("DELETE FROM messages WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    @RawQuery(observedEntities = {Message.class})
    abstract LiveData<List<MessageFull>> getAll(SupportSQLiteQuery query);
    @RawQuery(observedEntities = {Message.class, Metadata.class})
    abstract List<MessageFull> getNow(SupportSQLiteQuery query);
    @RawQuery(observedEntities = {Message.class, Metadata.class})
    abstract MessageFull getOneNow(SupportSQLiteQuery query);

    public LiveData<List<MessageFull>> getWithMetadataAndSenderName(int profileId, int messageType, String filter) {
        return getAll(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS senderFullName\n" +
                "FROM messages \n" +
                "LEFT JOIN teachers ON teachers.profileId = "+profileId+" AND teacherId = senderId\n" +
                "LEFT JOIN metadata ON messageId = thingId AND thingType = "+TYPE_MESSAGE+" AND metadata.profileId = "+profileId+"\n" +
                "WHERE messages.profileId = "+profileId+" AND messageType = "+messageType+" AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }

    public LiveData<List<MessageFull>> getWithMetadata(int profileId, int messageType, String filter) {
        return getAll(new SimpleSQLiteQuery("SELECT \n" +
                "* \n" +
                "FROM messages \n" +
                "LEFT JOIN metadata ON messageId = thingId AND thingType = "+TYPE_MESSAGE+" AND metadata.profileId = "+profileId+"\n" +
                "WHERE messages.profileId = "+profileId+" AND messageType = "+messageType+" AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }

    public MessageFull getById(int profileId, long messageId) {
        return getOneNow(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS senderFullName\n" +
                "FROM messages \n" +
                "LEFT JOIN teachers ON teachers.profileId = "+profileId+" AND teacherId = senderId\n" +
                "LEFT JOIN metadata ON messageId = thingId AND thingType = "+TYPE_MESSAGE+" AND metadata.profileId = "+profileId+"\n" +
                "WHERE messages.profileId = "+profileId+" AND messageId = "+messageId+"\n" +
                "ORDER BY addedDate DESC"));
    }

    public LiveData<List<MessageFull>> getReceived(int profileId) {
        return getWithMetadataAndSenderName(profileId, TYPE_RECEIVED, "1");
    }
    public LiveData<List<MessageFull>> getDeleted(int profileId) {
        return getWithMetadataAndSenderName(profileId, TYPE_DELETED, "1");
    }
    public LiveData<List<MessageFull>> getSent(int profileId) {
        return getWithMetadata(profileId, TYPE_SENT, "1");
    }

    public List<MessageFull> getReceivedNow(int profileId, String filter) {
        return getNow(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS senderFullName\n" +
                "FROM messages \n" +
                "LEFT JOIN teachers ON teachers.profileId = "+profileId+" AND teacherId = senderId\n" +
                "LEFT JOIN metadata ON messageId = thingId AND thingType = "+TYPE_MESSAGE+" AND metadata.profileId = "+profileId+"\n" +
                "WHERE messages.profileId = "+profileId+" AND messageType = 0 AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }
    public List<MessageFull> getReceivedNotNotifiedNow(int profileId) {
        return getReceivedNow(profileId, "notified = 0");
    }
}
