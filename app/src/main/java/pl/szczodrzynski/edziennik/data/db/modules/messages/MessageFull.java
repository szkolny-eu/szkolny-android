package pl.szczodrzynski.edziennik.data.db.modules.messages;

import androidx.annotation.Nullable;
import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.List;

public class MessageFull extends Message {
    public String senderFullName = null;
    @Ignore
    @Nullable
    public List<MessageRecipientFull> recipients = null;

    public MessageFull addRecipient(MessageRecipientFull recipient) {
        if (recipients == null)
            recipients = new ArrayList<>();
        recipients.add(recipient);
        return this;
    }

    // metadata
    public boolean seen;
    public boolean notified;
    public long addedDate;
}
