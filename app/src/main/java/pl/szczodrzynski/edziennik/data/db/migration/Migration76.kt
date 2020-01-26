/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration76 : Migration(75, 76) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE feedbackMessages RENAME TO _feedbackMessages;")
        database.execSQL("CREATE TABLE feedbackMessages (\n" +
                "\tmessageId INTEGER NOT NULL PRIMARY KEY,\n" +
                "\treceived INTEGER NOT NULL,\n" +
                "\ttext TEXT NOT NULL,\n" +
                "\tsenderName TEXT NOT NULL,\n" +
                "\tdeviceId TEXT DEFAULT NULL,\n" +
                "\tdeviceName TEXT DEFAULT NULL,\n" +
                "\tdevId INTEGER DEFAULT NULL,\n" +
                "\tdevImage TEXT DEFAULT NULL,\n" +
                "\tsentTime INTEGER NOT NULL\n" +
                ");")
        database.execSQL("INSERT INTO feedbackMessages (messageId, received, text, senderName, deviceId, deviceName, devId, devImage, sentTime)\n" +
                "SELECT messageId, received, text,\n" +
                "CASE fromUser IS NOT NULL WHEN 1 THEN CASE fromUserName IS NULL WHEN 1 THEN \"\" ELSE fromUserName END ELSE \"\" END AS senderName,\n" +
                "fromUser AS deviceId,\n" +
                "NULL AS deviceName,\n" +
                "CASE received AND fromUser IS NULL WHEN 1 THEN 100 ELSE NULL END AS devId,\n" +
                "NULL AS devImage,\n" +
                "sentTime\n" +
                "FROM _feedbackMessages;")
        database.execSQL("DROP TABLE _feedbackMessages;")
    }
}
