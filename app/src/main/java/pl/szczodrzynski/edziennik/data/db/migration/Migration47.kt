/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration47 : Migration(46, 47) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE profiles SET lastFullSync = 0")
    }
}
