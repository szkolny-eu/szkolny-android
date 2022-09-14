/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore

@Entity(tableName = "messageRecipients",
    primaryKeys = ["profileId", "messageRecipientId", "messageId"])
open class MessageRecipient {
    @JvmField
    var profileId = 0

    @JvmField
    @ColumnInfo(name = "messageRecipientId")
    var id: Long = -1


    @JvmField
    @ColumnInfo(name = "messageRecipientGlobalKey")
    var globalKey: String? = null

    @JvmField
    @ColumnInfo(name = "messageRecipientReplyId")
    var replyId: Long = -1

    /**
     * -1 for unknown
     * 0 for not read
     * 1 for read, date unknown
     * time in millis for read, date known
     */
    @JvmField
    @ColumnInfo(name = "messageRecipientReadDate")
    var readDate: Long = -1

    @JvmField
    var messageId: Long = 0

    constructor(profileId: Int, id: Long, replyId: Long, readDate: Long, messageId: Long) {
        this.profileId = profileId
        this.id = id
        this.replyId = replyId
        this.readDate = readDate
        this.messageId = messageId
    }

    @Ignore
    constructor(profileId: Int, globalKey: String, replyId: Long, readDate: Long, messageId: Long) {
        this.profileId = profileId
        this.globalKey = globalKey
        this.replyId = replyId
        this.readDate = readDate
        this.messageId = messageId
    }

    @Ignore
    constructor(profileId: Int, id: Long, messageId: Long) {
        this.profileId = profileId
        this.id = id
        this.messageId = messageId
    }

    @Ignore
    constructor()
}