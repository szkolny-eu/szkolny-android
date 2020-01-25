/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration56 : Migration(55, 56) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE IF NOT EXISTS endpointTimers (
                profileId INTEGER NOT NULL,
                endpointId INTEGER NOT NULL,
                endpointLastSync INTEGER DEFAULT NULL,
                endpointNextSync INTEGER NOT NULL DEFAULT 1,
                endpointViewId INTEGER DEFAULT NULL,
                PRIMARY KEY(profileId, endpointId))""")

        database.execSQL("""CREATE TABLE IF NOT EXISTS lessonRanges (
                profileId INTEGER NOT NULL,
                lessonRangeNumber INTEGER NOT NULL,
                lessonRangeStart TEXT NOT NULL,
                LessonRangeEnd TEXT NOT NULL,
                PRIMARY KEY(profileId, lessonRangeNumber))""")
    }
}
