/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType

class Migration58 : Migration(57, 58) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE metadata RENAME TO _metadata_old;")
        database.execSQL("DROP INDEX index_metadata_profileId_thingType_thingId;")
        database.execSQL("UPDATE _metadata_old SET thingType = ${MetadataType.HOMEWORK.id} WHERE thingType = ${MetadataType.EVENT.id} AND thingId IN (SELECT eventId FROM events WHERE eventType = -1)")
        database.execSQL("""CREATE TABLE metadata (
                profileId INTEGER NOT NULL,
                metadataId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                thingType INTEGER NOT NULL,
                thingId INTEGER NOT NULL,
                seen INTEGER NOT NULL,
                notified INTEGER NOT NULL,
                addedDate INTEGER NOT NULL)""")
        database.execSQL("INSERT INTO metadata SELECT * FROM (SELECT * FROM _metadata_old ORDER BY addedDate DESC) GROUP BY thingId;")
        database.execSQL("DROP TABLE _metadata_old;")
        database.execSQL("CREATE UNIQUE INDEX index_metadata_profileId_thingType_thingId ON metadata (profileId, thingType, thingId);")
    }
}
