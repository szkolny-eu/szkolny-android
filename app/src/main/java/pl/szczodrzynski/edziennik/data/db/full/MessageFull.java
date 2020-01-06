/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.full;

import androidx.annotation.Nullable;
import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.List;

import pl.szczodrzynski.edziennik.data.db.entity.Message;

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
