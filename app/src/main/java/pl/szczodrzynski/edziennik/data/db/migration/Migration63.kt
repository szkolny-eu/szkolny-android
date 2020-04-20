/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration63 : Migration(62, 63) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE profiles ADD COLUMN accountNameLong TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE profiles ADD COLUMN studentClassName TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE profiles ADD COLUMN studentSchoolYear TEXT DEFAULT NULL")
    }
}
