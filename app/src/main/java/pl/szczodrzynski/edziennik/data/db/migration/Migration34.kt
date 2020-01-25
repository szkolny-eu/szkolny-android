/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration34 : Migration(33, 34) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE messageRecipients")
        database.execSQL("""CREATE TABLE messageRecipients (
                profileId INTEGER NOT NULL,
                messageRecipientId INTEGER NOT NULL DEFAULT -1,
                messageRecipientReplyId INTEGER NOT NULL DEFAULT -1,
                messageRecipientReadDate INTEGER NOT NULL DEFAULT -1,
                messageId INTEGER NOT NULL,
                PRIMARY KEY(profileId, messageRecipientId, messageId))""")
    }
}
