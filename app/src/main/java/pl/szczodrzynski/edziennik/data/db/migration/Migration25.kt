/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration25 : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE announcements (
                profileId INTEGER NOT NULL,
                announcementId INTEGER NOT NULL,
                announcementSubject TEXT,
                announcementText TEXT,
                announcementStartDate TEXT,
                announcementEndDate TEXT,
                teacherId INTEGER NOT NULL,
                PRIMARY KEY(profileId, announcementId))""")
        database.execSQL("CREATE INDEX index_announcements_profileId ON announcements (profileId)")
    }
}
