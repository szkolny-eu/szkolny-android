package pl.szczodrzynski.edziennik.messages;

public class MessagesComposeInfo {
    /**
     * 0 means no attachments.
     * -1 means unlimited number.
     */
    public int maxAttachmentNumber = 0;
    /**
     * -1 means unlimited size.
     */
    public long attachmentSizeLimit = 0;
    /**
     * -1 means unlimited length.
     */
    public int maxSubjectLength = -1;
    /**
     * -1 means unlimited length.
     */
    public int maxBodyLength = -1;

    public MessagesComposeInfo(int maxAttachmentNumber, long attachmentSizeLimit, int maxSubjectLength, int maxBodyLength) {
        this.maxAttachmentNumber = maxAttachmentNumber;
        this.attachmentSizeLimit = attachmentSizeLimit;
        this.maxSubjectLength = maxSubjectLength;
        this.maxBodyLength = maxBodyLength;
    }
}
