/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "messageRecipients",
        primaryKeys = {"profileId", "messageRecipientId", "messageId"})
public class MessageRecipient {
    public int profileId;

    @ColumnInfo(name = "messageRecipientId")
    public long id = -1;
    @ColumnInfo(name = "messageRecipientReplyId")
    public long replyId = -1;
    /**
     * -1 for unknown
     * 0 for not read
     * 1 for read, date unknown
     * time in millis for read, date known
     */
    @ColumnInfo(name = "messageRecipientReadDate")
    public long readDate = -1;

    public long messageId;

    public MessageRecipient(int profileId, long id, long replyId, long readDate, long messageId) {
        this.profileId = profileId;
        this.id = id;
        this.replyId = replyId;
        this.readDate = readDate;
        this.messageId = messageId;
    }

    @Ignore
    public MessageRecipient(int profileId, long id, long messageId) {
        this.profileId = profileId;
        this.id = id;
        this.messageId = messageId;
    }

    @Ignore
    public MessageRecipient() {
    }
}
