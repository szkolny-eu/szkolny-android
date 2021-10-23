/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-15.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.COLOR_ELEARNING
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_ELEARNING
import pl.szczodrzynski.edziennik.data.db.entity.Event.Companion.TYPE_INFORMATION
import pl.szczodrzynski.edziennik.data.db.entity.EventType.Companion.SOURCE_DEFAULT
import pl.szczodrzynski.edziennik.data.db.entity.EventType.Companion.SOURCE_REGISTER
import pl.szczodrzynski.edziennik.ext.getInt

class Migration92 : Migration(91, 92) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // make eventTypeName not nullable
        database.execSQL("ALTER TABLE eventTypes RENAME TO _eventTypes;")
        database.execSQL("CREATE TABLE eventTypes (" +
                "profileId INTEGER NOT NULL, " +
                "eventType INTEGER NOT NULL, " +
                "eventTypeName TEXT NOT NULL, " +
                "eventTypeColor INTEGER NOT NULL, " +
                "PRIMARY KEY(profileId,eventType)" +
                ");")
        database.execSQL("INSERT INTO eventTypes " +
                "(profileId, eventType, eventTypeName, eventTypeColor) " +
                "SELECT profileId, eventType, eventTypeName, eventTypeColor " +
                "FROM _eventTypes;")
        database.execSQL("DROP TABLE _eventTypes;")

        // add columns for order and source
        database.execSQL("ALTER TABLE eventTypes ADD COLUMN eventTypeOrder INTEGER NOT NULL DEFAULT 0;")
        database.execSQL("ALTER TABLE eventTypes ADD COLUMN eventTypeSource INTEGER NOT NULL DEFAULT 0;")

        // migrate existing types to show correct order and source
        database.execSQL("UPDATE eventTypes SET eventTypeOrder = eventType + 102;")
        database.execSQL("UPDATE eventTypes SET eventTypeSource = $SOURCE_REGISTER WHERE eventType > $TYPE_INFORMATION;")

        // add new e-learning type
        val cursor = database.query("SELECT profileId FROM profiles;")
        cursor.use {
            while (it.moveToNext()) {
                val values = ContentValues().apply {
                    put("profileId", it.getInt("profileId"))
                    put("eventType", TYPE_ELEARNING)
                    put("eventTypeName", "lekcja online")
                    put("eventTypeColor", COLOR_ELEARNING)
                    put("eventTypeOrder", 100)
                    put("eventTypeSource", SOURCE_DEFAULT)
                }

                database.insert("eventTypes", SQLiteDatabase.CONFLICT_REPLACE, values)
            }
        }
    }
}
