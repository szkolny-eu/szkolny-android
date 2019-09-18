package pl.szczodrzynski.edziennik.datamodels;

public class MessageRecipientFull extends MessageRecipient {
    public String fullName = null;

    public MessageRecipientFull(int profileId, long id, long replyId, long readDate, long messageId) {
        super(profileId, id, replyId, readDate, messageId);
    }

    public MessageRecipientFull(int profileId, long id, long messageId) {
        super(profileId, id, messageId);
    }

    public MessageRecipientFull() {
        super();
    }
}
