/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-16.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration98 : Migration(97, 98) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // timetable colors - override color in lesson object
        database.execSQL("ALTER TABLE timetable ADD COLUMN color INT DEFAULT NULL;")
    }
}
