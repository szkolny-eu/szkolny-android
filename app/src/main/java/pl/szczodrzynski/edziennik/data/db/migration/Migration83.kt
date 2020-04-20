/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration83 : Migration(82, 83) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE events ADD COLUMN keep INTEGER NOT NULL DEFAULT 1")
    }
}
