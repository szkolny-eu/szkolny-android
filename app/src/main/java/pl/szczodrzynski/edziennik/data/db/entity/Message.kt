/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import pl.szczodrzynski.edziennik.isNotNullNorEmpty

@Entity(tableName = "messages",
        primaryKeys = ["profileId", "messageId"],
        indices = [
            Index(value = ["profileId", "messageType"])
        ])
open class Message(
        val profileId: Int,
        @ColumnInfo(name = "messageId")
        val id: Long,
        @ColumnInfo(name = "messageType")
        var type: Int,

        @ColumnInfo(name = "messageSubject")
        var subject: String,
        @ColumnInfo(name = "messageBody")
        var body: String?,

        /**
         * Keep in mind that this being null does NOT
         * necessarily mean the message is sent.
         */
        var senderId: Long?,
        var addedDate: Long = System.currentTimeMillis()
) : Keepable() {
    companion object {
        const val TYPE_RECEIVED = 0
        const val TYPE_SENT = 1
        const val TYPE_DELETED = 2
        const val TYPE_DRAFT = 3
    }

    @ColumnInfo(name = "messageIsPinned")
    var isStarred: Boolean = false

    var hasAttachments = false // if the attachments are not yet downloaded but we already know there are some
        get() = field || attachmentIds.isNotNullNorEmpty()
    var attachmentIds: MutableList<Long>? = null
    var attachmentNames: MutableList<String>? = null
    var attachmentSizes: MutableList<Long>? = null

    @Ignore
    var showAsUnseen: Boolean? = null

    /**
     * Add an attachment
     * @param id attachment ID
     * @param name file name incl. extension
     * @param size file size or -1 if unknown
     * @return a Message to which the attachment has been added
     */
    fun addAttachment(id: Long, name: String, size: Long): Message {
        if (attachmentIds == null) attachmentIds = mutableListOf()
        if (attachmentNames == null) attachmentNames = mutableListOf()
        if (attachmentSizes == null) attachmentSizes = mutableListOf()
        attachmentIds?.add(id)
        attachmentNames?.add(name)
        attachmentSizes?.add(size)
        return this
    }

    fun clearAttachments() {
        attachmentIds = null
        attachmentNames = null
        attachmentSizes = null
    }
}
