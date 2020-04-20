/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration76 : Migration(75, 76) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE feedbackMessages RENAME TO _feedbackMessages;")
        database.execSQL("""CREATE TABLE feedbackMessages (
                messageId INTEGER NOT NULL PRIMARY KEY,
                received INTEGER NOT NULL,
                text TEXT NOT NULL,
                senderName TEXT NOT NULL,
                deviceId TEXT DEFAULT NULL,
                deviceName TEXT DEFAULT NULL,
                devId INTEGER DEFAULT NULL,
                devImage TEXT DEFAULT NULL,
                sentTime INTEGER NOT NULL);""")
        database.execSQL("""INSERT INTO feedbackMessages (messageId, received, text, senderName, deviceId, deviceName, devId, devImage, sentTime)
                SELECT messageId, received, text,
                CASE fromUser IS NOT NULL WHEN 1 THEN CASE fromUserName IS NULL WHEN 1 THEN "" ELSE fromUserName END ELSE "" END AS senderName,
                fromUser AS deviceId,
                NULL AS deviceName,
                CASE received AND fromUser IS NULL WHEN 1 THEN 100 ELSE NULL END AS devId,
                NULL AS devImage,
                sentTime
                FROM _feedbackMessages;""")
        database.execSQL("DROP TABLE _feedbackMessages;")
    }
}
