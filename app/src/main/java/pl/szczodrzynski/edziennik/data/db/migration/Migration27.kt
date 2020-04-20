/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration27 : Migration(26, 27) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE grades ADD gradeValueMax REAL NOT NULL DEFAULT 0;")
    }
}
