/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-25.
 * Copyright (c) Franciszek Pilch 2024-06-14.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration101 : Migration(100, 101) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE grades ADD COLUMN code TEXT DEFAULT NULL;")
    }
}