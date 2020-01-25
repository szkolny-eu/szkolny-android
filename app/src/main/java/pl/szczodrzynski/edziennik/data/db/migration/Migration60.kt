/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration60 : Migration(59, 60) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                `text` TEXT NOT NULL,
                `type` INTEGER NOT NULL,
                profileId INTEGER DEFAULT NULL,
                profileName TEXT DEFAULT NULL,
                posted INTEGER NOT NULL DEFAULT 0,
                viewId INTEGER DEFAULT NULL,
                extras TEXT DEFAULT NULL,
                addedDate INTEGER NOT NULL)""")
        database.execSQL("ALTER TABLE profiles ADD COLUMN disabledNotifications TEXT DEFAULT NULL")
    }
}
