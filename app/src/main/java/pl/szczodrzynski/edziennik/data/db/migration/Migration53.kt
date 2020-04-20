/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration53 : Migration(52, 53) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE IF NOT EXISTS teacherAbsence (
                profileId INTEGER NOT NULL,
                teacherAbsenceId INTEGER NOT NULL,
                teacherId INTEGER NOT NULL,
                teacherAbsenceType INTEGER NOT NULL,
                teacherAbsenceDateFrom TEXT NOT NULL,
                teacherAbsenceDateTo TEXT NOT NULL,
                PRIMARY KEY(profileId, teacherAbsenceId))""")
    }
}
