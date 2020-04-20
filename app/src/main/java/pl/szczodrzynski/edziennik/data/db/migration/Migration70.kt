/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration70 : Migration(69, 70) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE announcements ADD COLUMN announcementIdString TEXT DEFAULT NULL")
        database.execSQL("DELETE FROM announcements")
    }
}
