/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration62 : Migration(61, 62) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE IF NOT EXISTS classrooms (
                profileId INTEGER NOT NULL,
                id INTEGER NOT NULL,
                name TEXT NOT NULL,
                PRIMARY KEY(profileId, id))""")

        database.execSQL("""CREATE TABLE IF NOT EXISTS noticeTypes (
                profileId INTEGER NOT NULL,
                id INTEGER NOT NULL,
                name TEXT NOT NULL,
                PRIMARY KEY(profileId, id))""")

        database.execSQL("""CREATE TABLE IF NOT EXISTS attendanceTypes (
                profileId INTEGER NOT NULL,
                id INTEGER NOT NULL,
                name TEXT NOT NULL,
                type INTEGER NOT NULL,
                color INTEGER NOT NULL,
                PRIMARY KEY(profileId, id))""")
    }
}
