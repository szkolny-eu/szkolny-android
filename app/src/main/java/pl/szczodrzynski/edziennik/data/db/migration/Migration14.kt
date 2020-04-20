/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration14 : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE loginStores (loginStoreId INTEGER NOT NULL, loginStoreType INTEGER NOT NULL, loginStoreData TEXT, PRIMARY KEY(loginStoreId));")
    }
}
