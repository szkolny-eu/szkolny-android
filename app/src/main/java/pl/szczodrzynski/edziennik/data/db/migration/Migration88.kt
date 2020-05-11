/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-9.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration88 : Migration(87, 88) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE endpointTimers SET endpointLastSync = 0 WHERE endpointId IN (1030, 1040, 1050, 1060, 1070, 1080);")
        database.execSQL("UPDATE profiles SET empty = 1 WHERE loginStoreType = 4")
    }
}
