/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration66 : Migration(65, 66) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE config;")
        database.execSQL("""CREATE TABLE config (
                profileId INTEGER NOT NULL DEFAULT -1,
                `key` TEXT NOT NULL,
                value TEXT,
                PRIMARY KEY(profileId, `key`))""")
    }
}
