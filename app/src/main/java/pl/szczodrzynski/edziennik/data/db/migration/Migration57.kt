/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration57 : Migration(56, 57) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE gradeCategories ADD type INTEGER NOT NULL DEFAULT 0")
    }
}
