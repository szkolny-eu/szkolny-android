/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration43 : Migration(42, 43) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE profiles ADD lastFullSync INTEGER NOT NULL DEFAULT 0")
    }
}
