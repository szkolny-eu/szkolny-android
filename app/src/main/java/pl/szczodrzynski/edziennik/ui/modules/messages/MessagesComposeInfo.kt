package pl.szczodrzynski.edziennik.ui.modules.messages

class MessagesComposeInfo(
        /**
         * 0 means no attachments.
         * -1 means unlimited number.
         */
        @JvmField var maxAttachmentNumber: Int,
        /**
         * -1 means unlimited size.
         */
        var attachmentSizeLimit: Long,
        /**
         * -1 means unlimited length.
         */
        var maxSubjectLength: Int,
        /**
         * -1 means unlimited length.
         */
        var maxBodyLength: Int
)