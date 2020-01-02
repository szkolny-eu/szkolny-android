package pl.szczodrzynski.edziennik.data.db.modules.messages

import androidx.room.Ignore
import pl.szczodrzynski.edziennik.fixName

class MessageRecipientFull : MessageRecipient {
    var fullName: String? = ""
        get() {
            return field?.fixName() ?: ""
        }

    @Ignore
    constructor(profileId: Int, id: Long, replyId: Long, readDate: Long, messageId: Long) : super(profileId, id, replyId, readDate, messageId) {}
    @Ignore
    constructor(profileId: Int, id: Long, messageId: Long) : super(profileId, id, messageId) {}
    constructor() : super() {}
}