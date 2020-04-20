/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration18 : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE events ADD eventBlacklisted INTEGER NOT NULL DEFAULT 0;")
    }
}
