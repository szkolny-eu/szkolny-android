/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-4.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration84 : Migration(83, 84) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // The Message Update
        database.execSQL("ALTER TABLE messages RENAME TO _messages;")
        database.execSQL("""CREATE TABLE messages (
            profileId INTEGER NOT NULL,
            messageId INTEGER NOT NULL,
            messageType INTEGER NOT NULL,
            messageSubject TEXT NOT NULL,
            messageBody TEXT,
            senderId INTEGER,
            messageIsPinned INTEGER NOT NULL DEFAULT 0,
            hasAttachments INTEGER NOT NULL DEFAULT 0,
            attachmentIds TEXT DEFAULT NULL,
            attachmentNames TEXT DEFAULT NULL,
            attachmentSizes TEXT DEFAULT NULL,
            keep INTEGER NOT NULL DEFAULT 1,
            PRIMARY KEY(profileId, messageId)
        )""")
        database.execSQL("DROP INDEX IF EXISTS index_messages_profileId")
        database.execSQL("CREATE INDEX index_messages_profileId_messageType ON messages (profileId, messageType)")
        database.execSQL("""
            INSERT INTO messages (profileId, messageId, messageType, messageSubject, messageBody, senderId, hasAttachments, attachmentIds, attachmentNames, attachmentSizes)
            SELECT profileId, messageId, messageType, messageSubject, messageBody,
            CASE senderId WHEN -1 THEN NULL ELSE senderId END,
            overrideHasAttachments, attachmentIds, attachmentNames, attachmentSizes
            FROM _messages
        """)
        database.execSQL("DROP TABLE _messages")
    }
}
