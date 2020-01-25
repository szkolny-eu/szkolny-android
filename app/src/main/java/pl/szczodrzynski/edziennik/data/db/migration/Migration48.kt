/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration48 : Migration(47, 48) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE notices ADD points REAL NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE notices ADD category TEXT DEFAULT NULL")
    }
}
