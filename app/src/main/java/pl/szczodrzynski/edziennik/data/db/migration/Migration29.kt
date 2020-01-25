/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration29 : Migration(28, 29) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE feedbackMessages (messageId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, received INTEGER NOT NULL DEFAULT 0, sentTime INTEGER NOT NULL, `text` TEXT)")
    }
}
