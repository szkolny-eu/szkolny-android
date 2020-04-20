/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration52 : Migration(51, 52) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE teachers ADD teacherTypeDescription TEXT DEFAULT NULL")
    }
}
