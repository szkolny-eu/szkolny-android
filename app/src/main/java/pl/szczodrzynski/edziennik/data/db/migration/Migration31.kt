/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration31 : Migration(30, 31) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE messages (
                profileId INTEGER NOT NULL,
                messageId INTEGER NOT NULL,
                messageSubject TEXT,
                messageBody TEXT DEFAULT NULL,
                messageType INTEGER NOT NULL DEFAULT 0,
                senderId INTEGER NOT NULL DEFAULT -1,
                senderReplyId INTEGER NOT NULL DEFAULT -1,
                recipientIds TEXT DEFAULT NULL,
                recipientReplyIds TEXT DEFAULT NULL,
                readByRecipientDates TEXT DEFAULT NULL,
                overrideHasAttachments INTEGER NOT NULL DEFAULT 0,
                attachmentIds TEXT DEFAULT NULL,
                attachmentNames TEXT DEFAULT NULL,
                PRIMARY KEY(profileId, messageId))""")
        database.execSQL("CREATE INDEX index_messages_profileId ON messages (profileId)")
    }
}
