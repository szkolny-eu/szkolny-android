/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration28 : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE gradeCategories (profileId INTEGER NOT NULL, categoryId INTEGER NOT NULL, weight REAL NOT NULL, color INTEGER NOT NULL, `text` TEXT, columns TEXT, valueFrom REAL NOT NULL, valueTo REAL NOT NULL, PRIMARY KEY(profileId, categoryId));")
        database.execSQL("ALTER TABLE grades ADD gradeComment TEXT;")
    }
}
