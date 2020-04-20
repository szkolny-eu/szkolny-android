/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration54 : Migration(53, 54) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE teacherAbsence ADD teacherAbsenceTimeFrom TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE teacherAbsence ADD teacherAbsenceTimeTo TEXT DEFAULT NULL")
    }
}
