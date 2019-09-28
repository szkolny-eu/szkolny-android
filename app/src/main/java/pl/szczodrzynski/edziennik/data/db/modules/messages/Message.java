package pl.szczodrzynski.edziennik.data.db.modules.messages;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

@Entity(tableName = "messages",
        primaryKeys = {"profileId", "messageId"},
        indices = {@Index(value = {"profileId"})})
public class Message {
    int profileId;

    @ColumnInfo(name = "messageId")
    public long id;

    @ColumnInfo(name = "messageSubject")
    public String subject;
    @Nullable
    @ColumnInfo(name = "messageBody")
    public String body = null;

    public static final int TYPE_RECEIVED = 0;
    public static final int TYPE_SENT = 1;
    public static final int TYPE_DELETED = 2;
    public static final int TYPE_DRAFT = 3;
    @ColumnInfo(name = "messageType")
    public int type = TYPE_RECEIVED;

    public long senderId = -1; // -1 for sent messages
    public long senderReplyId = -1;
    public boolean overrideHasAttachments = false; // if the attachments are not yet downloaded but we already know there are some
    public List<Long> attachmentIds = null;
    public List<String> attachmentNames = null;
    public List<Long> attachmentSizes = null;

    @Ignore
    public Message() {}

    public Message(int profileId, long id, String subject, @Nullable String body, int type, long senderId, long senderReplyId) {
        this.profileId = profileId;
        this.id = id;
        this.subject = subject;
        this.body = body;
        this.type = type;
        this.senderId = senderId;
        this.senderReplyId = senderReplyId;
    }

    /**
     * Add an attachment
     * @param id attachment ID
     * @param name file name incl. extension
     * @param size file size or -1 if unknown
     * @return a Message to which the attachment has been added
     */
    public Message addAttachment(long id, String name, long size) {
        if (attachmentIds == null)
            attachmentIds = new ArrayList<>();
        if (attachmentNames == null)
            attachmentNames = new ArrayList<>();
        if (attachmentSizes == null)
            attachmentSizes = new ArrayList<>();
        attachmentIds.add(id);
        attachmentNames.add(name);
        attachmentSizes.add(size);
        return this;
    }

    public void clearAttachments() {
        attachmentIds = null;
        attachmentNames = null;
        attachmentSizes = null;
    }

    public Message setHasAttachments() {
        overrideHasAttachments = true;
        return this;
    }

    public boolean hasAttachments() {
        return overrideHasAttachments || (attachmentIds != null && attachmentIds.size() > 0);
    }
}
