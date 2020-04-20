/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration21 : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE grades ADD gradeCategory TEXT")
        database.execSQL("ALTER TABLE grades ADD gradeColor INTEGER NOT NULL DEFAULT -1")
        database.execSQL("DROP TABLE gradeCategories")
    }
}
