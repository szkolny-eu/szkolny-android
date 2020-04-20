/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration32 : Migration(31, 32) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE messages ADD attachmentSizes TEXT DEFAULT NULL")
    }
}
