/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration67 : Migration(66, 67) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM grades WHERE (gradeId=-1 OR gradeId=-2) AND gradeType=20")
        database.execSQL("DELETE FROM metadata WHERE (thingId=-1 OR thingId=-2) AND thingType=1")
        database.execSQL("ALTER TABLE gradeCategories RENAME TO _gradeCategories")

        database.execSQL("""CREATE TABLE gradeCategories (
                profileId INTEGER NOT NULL,
                categoryId INTEGER NOT NULL,
                weight REAL NOT NULL,
                color INTEGER NOT NULL,
                `text` TEXT,
                columns TEXT,
                valueFrom REAL NOT NULL,
                valueTo REAL NOT NULL,
                type INTEGER NOT NULL,
                PRIMARY KEY(profileId, categoryId, type))""")

        database.execSQL("""INSERT INTO gradeCategories (profileId, categoryId, weight, color,
                `text`, columns, valueFrom, valueTo, type)
                SELECT profileId, categoryId, weight, color, `text`, columns, valueFrom,
                valueTo, type FROM _gradeCategories""")
        database.execSQL("DROP TABLE _gradeCategories")
    }
}
