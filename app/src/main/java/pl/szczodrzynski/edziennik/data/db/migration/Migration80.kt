/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-27.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration80 : Migration(79, 80) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // The Homework Update
        database.execSQL("ALTER TABLE events RENAME TO _events;")
        database.execSQL("""CREATE TABLE events (
            profileId INTEGER NOT NULL,
            eventId INTEGER NOT NULL,
            eventDate TEXT NOT NULL,
            eventTime TEXT,
            eventTopic TEXT NOT NULL,
            eventColor INTEGER,
            eventType INTEGER NOT NULL,
            teacherId INTEGER NOT NULL,
            subjectId INTEGER NOT NULL,
            teamId INTEGER NOT NULL,
            eventAddedManually INTEGER NOT NULL DEFAULT 0,
            eventSharedBy TEXT DEFAULT NULL,
            eventSharedByName TEXT DEFAULT NULL,
            eventBlacklisted INTEGER NOT NULL DEFAULT 0,
            homeworkBody TEXT DEFAULT NULL,
            attachmentIds TEXT DEFAULT NULL,
            attachmentNames TEXT DEFAULT NULL,
            PRIMARY KEY(profileId, eventId)
        )""")
        database.execSQL("DROP INDEX IF EXISTS index_events_profileId_eventDate_eventStartTime")
        database.execSQL("DROP INDEX IF EXISTS index_events_profileId_eventType")
        database.execSQL("CREATE INDEX index_events_profileId_eventDate_eventTime ON events (profileId, eventDate, eventTime)")
        database.execSQL("CREATE INDEX index_events_profileId_eventType ON events (profileId, eventType)")
        database.execSQL("""
            INSERT INTO events (profileId, eventId, eventDate, eventTime, eventTopic, eventColor, eventType, teacherId, subjectId, teamId, eventAddedManually, eventSharedBy, eventSharedByName, eventBlacklisted)
            SELECT profileId, eventId, eventDate, eventStartTime, eventTopic,
            CASE eventColor WHEN -1 THEN NULL ELSE eventColor END,
            eventType, teacherId, subjectId, teamId,
            eventAddedManually, eventSharedBy, eventSharedByName, eventBlacklisted
            FROM _events
        """)
        database.execSQL("DROP TABLE _events")
    }
}
