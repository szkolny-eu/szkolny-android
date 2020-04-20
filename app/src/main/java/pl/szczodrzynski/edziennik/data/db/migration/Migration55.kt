/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration55 : Migration(54, 55) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 2019-10-21 for merge compatibility between 3.1.1 and api-v2
        // moved to Migration 55->56
    }
}
