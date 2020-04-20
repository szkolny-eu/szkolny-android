/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration61 : Migration(60, 61) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE IF NOT EXISTS teacherAbsenceTypes (
                profileId INTEGER NOT NULL,
                teacherAbsenceTypeId INTEGER NOT NULL,
                teacherAbsenceTypeName TEXT NOT NULL,
                PRIMARY KEY(profileId, teacherAbsenceTypeId))""")
        database.execSQL("ALTER TABLE teacherAbsence ADD COLUMN teacherAbsenceName TEXT DEFAULT NULL")
    }
}
