/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration69 : Migration(68, 69) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE loginStores ADD COLUMN loginStoreMode INTEGER NOT NULL DEFAULT 0")
    }
}
