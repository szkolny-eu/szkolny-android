/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration35 : Migration(34, 35) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE teachers ADD teacherLoginId TEXT DEFAULT NULL;")
    }
}
