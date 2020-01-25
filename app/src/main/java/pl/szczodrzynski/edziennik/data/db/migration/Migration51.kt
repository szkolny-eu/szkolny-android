/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration51 : Migration(50, 51) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE profiles ADD lastReceiversSync INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE teachers ADD teacherType INTEGER NOT NULL DEFAULT 0")
    }
}
