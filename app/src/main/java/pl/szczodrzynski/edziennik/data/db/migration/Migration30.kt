/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration30 : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE feedbackMessages ADD fromUser TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE feedbackMessages ADD fromUserName TEXT DEFAULT NULL")
    }
}
