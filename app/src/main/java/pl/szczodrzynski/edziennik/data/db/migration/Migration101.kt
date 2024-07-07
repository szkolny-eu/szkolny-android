/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-25.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration101 : Migration(100, 101) {

    override fun migrate(db: SupportSQLiteDatabase) {
        // remove old debugLogs table
        db.execSQL("DROP TABLE debugLogs")
        // add new logs table
        db.execSQL(
            "CREATE TABLE logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "priority INTEGER NOT NULL, " +
                    "tag TEXT, " +
                    "message TEXT NOT NULL)"
        )
        // create index for logs table
        db.execSQL("CREATE INDEX IF NOT EXISTS index_logs_timestamp ON logs (timestamp)")
    }
}
