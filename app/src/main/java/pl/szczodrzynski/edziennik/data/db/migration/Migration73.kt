/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration73 : Migration(72, 73) {
    override fun migrate(database: SupportSQLiteDatabase) { // Mark as seen all lucky number metadata.
        database.execSQL("UPDATE metadata SET seen=1 WHERE thingType=10")
        database.execSQL("DROP TABLE lessons")
        database.execSQL("DROP TABLE lessonChanges")
    }
}
