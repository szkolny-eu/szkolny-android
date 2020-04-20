/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.annotation.SelectiveDao
import pl.szczodrzynski.edziennik.annotation.UpdateSelective
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.full.MessageFull

@Dao
@SelectiveDao(db = AppDb::class)
abstract class MessageDao : BaseDao<Message, MessageFull> {
    companion object {
        private const val QUERY = """
            SELECT 
            *, 
            teachers.teacherName ||" "|| teachers.teacherSurname AS senderName
            FROM messages
            LEFT JOIN teachers ON teachers.profileId = messages.profileId AND teacherId = senderId
            LEFT JOIN metadata ON messageId = thingId AND thingType = ${Metadata.TYPE_MESSAGE} AND metadata.profileId = messages.profileId
        """

        private const val ORDER_BY = """ORDER BY messageIsPinned, addedDate DESC"""
    }

    private val selective by lazy { MessageDaoSelective(App.db) }

    @RawQuery(observedEntities = [Message::class])
    abstract override fun getRaw(query: SupportSQLiteQuery): LiveData<List<MessageFull>>

    @UpdateSelective(primaryKeys = ["profileId", "messageId"], skippedColumns = ["messageType", "messageBody", "messageIsPinned", "attachmentIds", "attachmentNames", "attachmentSizes"])
    override fun update(item: Message) = selective.update(item)
    override fun updateAll(items: List<Message>) = selective.updateAll(items)

    // CLEAR
    @Query("DELETE FROM messages WHERE profileId = :profileId")
    abstract override fun clear(profileId: Int)

    // GET ALL - LIVE DATA
    fun getAll(profileId: Int) =
            getRaw("$QUERY WHERE messages.profileId = $profileId $ORDER_BY")
    fun getAllByType(profileId: Int, type: Int) =
            getRaw("$QUERY WHERE messages.profileId = $profileId AND messageType = $type $ORDER_BY")
    fun getReceived(profileId: Int) = getAllByType(profileId, Message.TYPE_RECEIVED)
    fun getSent(profileId: Int) = getAllByType(profileId, Message.TYPE_SENT)
    fun getDeleted(profileId: Int) = getAllByType(profileId, Message.TYPE_DELETED)
    fun getDraft(profileId: Int) = getAllByType(profileId, Message.TYPE_DRAFT)

    // GET ALL - NOW
    fun getAllNow(profileId: Int) =
            getRawNow("$QUERY WHERE messages.profileId = $profileId $ORDER_BY")
    fun getNotNotifiedNow() =
            getRawNow("$QUERY WHERE notified = 0 AND messageType = ${Message.TYPE_RECEIVED} $ORDER_BY")

    // GET ONE - NOW
    fun getByIdNow(profileId: Int, id: Long) =
            getOneNow("$QUERY WHERE messages.profileId = $profileId AND messageId = $id")
}
