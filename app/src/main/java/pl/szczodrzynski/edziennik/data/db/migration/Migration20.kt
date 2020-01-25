/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration20 : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE luckyNumbers (
                profileId INTEGER NOT NULL,
                luckyNumberDate TEXT NOT NULL,
                luckyNumber INTEGER NOT NULL,
                PRIMARY KEY(profileId, luckyNumberDate));""")
    }
}
