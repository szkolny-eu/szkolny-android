/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration33 : Migration(32, 33) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE messages")
        database.execSQL("""CREATE TABLE messages (
                profileId INTEGER NOT NULL,
                messageId INTEGER NOT NULL,
                messageSubject TEXT,
                messageBody TEXT DEFAULT NULL,
                messageType INTEGER NOT NULL DEFAULT 0,
                senderId INTEGER NOT NULL DEFAULT -1,
                senderReplyId INTEGER NOT NULL DEFAULT -1,
                overrideHasAttachments INTEGER NOT NULL DEFAULT 0,
                attachmentIds TEXT DEFAULT NULL,
                attachmentNames TEXT DEFAULT NULL,
                attachmentSizes TEXT DEFAULT NULL,
                PRIMARY KEY(profileId, messageId))""")
        database.execSQL("CREATE INDEX index_messages_profileId ON messages (profileId)")

        database.execSQL("""CREATE TABLE messageRecipients (
                profileId INTEGER NOT NULL,
                messageRecipientId INTEGER NOT NULL DEFAULT -1,
                messageRecipientReplyId INTEGER NOT NULL DEFAULT -1,
                messageRecipientReadDate INTEGER NOT NULL DEFAULT -1,
                messageId INTEGER NOT NULL,
                PRIMARY KEY(profileId, messageRecipientId))""")
    }
}
