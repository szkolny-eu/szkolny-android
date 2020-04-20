/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration12 : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("PRAGMA foreign_keys=off;")
        database.execSQL("ALTER TABLE teams RENAME TO _teams_old;")
        database.execSQL("CREATE TABLE teams (profileId INTEGER NOT NULL, teamId INTEGER NOT NULL, teamType INTEGER NOT NULL, teamName TEXT, teamTeacherId INTEGER NOT NULL, PRIMARY KEY(profileId, teamId));")
        database.execSQL("INSERT INTO teams (profileId, teamId, teamType, teamName, teamTeacherId) SELECT profileId, teamId, teamType, teamName, teacherId FROM _teams_old;")
        database.execSQL("DROP TABLE _teams_old;")
        database.execSQL("PRAGMA foreign_keys=on;")
    }
}
