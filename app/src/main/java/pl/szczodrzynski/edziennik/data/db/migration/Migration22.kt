/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration22 : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE eventTypes (
                profileId INTEGER NOT NULL,
                eventType INTEGER NOT NULL,
                eventTypeName TEXT,
                eventTypeColor INTEGER NOT NULL,
                PRIMARY KEY(profileId, eventType));""")
    }
}
