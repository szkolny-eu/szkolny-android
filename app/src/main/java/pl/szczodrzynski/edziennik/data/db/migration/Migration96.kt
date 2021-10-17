/*
 * Copyright (c) Antoni Czaplicki 2021-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration96 : Migration(95, 96) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // timetable - is extra flag
        database.execSQL("ALTER TABLE teachers ADD COLUMN subjects TEXT;")
    }
}
