/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration59 : Migration(58, 59) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE metadata SET addedDate = addedDate*1000 WHERE addedDate < 10000000000;")
        database.execSQL("""INSERT INTO metadata (profileId, thingType, thingId, seen, notified, addedDate)
                SELECT profileId,
                10 AS thingType,
                luckyNumberDate*10000+substr(luckyNumberDate, 6)*100+substr(luckyNumberDate, 9) AS thingId,
                1 AS seen,
                1 AS notified,
                CAST(strftime('%s', luckyNumberDate) AS INT)*1000 AS addedDate
                FROM luckyNumbers""")

        database.execSQL("ALTER TABLE luckyNumbers RENAME TO _old_luckyNumbers;")
        database.execSQL("""CREATE TABLE luckyNumbers (
                profileId INTEGER NOT NULL,
                luckyNumberDate INTEGER NOT NULL,
                luckyNumber INTEGER NOT NULL,
                PRIMARY KEY(profileId, luckyNumberDate))""")

        database.execSQL("""INSERT INTO luckyNumbers (profileId, luckyNumberDate, luckyNumber)
                SELECT profileId,
                luckyNumberDate * 10000 + substr(luckyNumberDate, 6) * 100 + substr(luckyNumberDate, 9) AS luckyNumberDate,
                luckyNumber FROM _old_luckyNumbers;""")
        database.execSQL("DROP TABLE _old_luckyNumbers;")
    }
}
