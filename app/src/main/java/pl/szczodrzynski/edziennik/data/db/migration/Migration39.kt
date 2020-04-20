/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration39 : Migration(38, 39) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE debugLogs (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `text` TEXT)")
    }
}
