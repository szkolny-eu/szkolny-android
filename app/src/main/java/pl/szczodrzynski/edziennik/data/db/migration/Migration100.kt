/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-25.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration100 : Migration(99, 100) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // add Note Owner ID to Lesson, to make it profileId-independent
        // calculate the new owner ID based on the old ID
        database.execSQL("ALTER TABLE timetable ADD COLUMN ownerId INT NOT NULL DEFAULT 0;")
        // set new ID for actual lessons
        database.execSQL("UPDATE timetable SET ownerId = ROUND((id & ~65535) / 500000.0) * 300000;")
        // copy the old ID (date value) for NO_LESSONS
        database.execSQL("UPDATE timetable SET ownerId = id WHERE type = -1;")
        // update ID for notes as well
        database.execSQL("UPDATE notes SET noteOwnerId = ROUND((noteOwnerId & ~65535) / 500000.0) * 300000 WHERE noteOwnerType = 'LESSON' AND noteOwnerId > 2000000000000;")
        // force full app sync to download notes with new IDs
        database.execSQL("DELETE FROM config WHERE `key` = 'hash';")
        database.execSQL("DELETE FROM config WHERE `key` = 'lastAppSync';")
    }
}
