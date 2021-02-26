/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-26.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration90 : Migration(89, 90) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // get all profiles using Vulcan/Hebe
        database.execSQL("CREATE TABLE _90_ids (id INTEGER NOT NULL);")
        database.execSQL("INSERT INTO _90_ids SELECT profileId FROM profiles JOIN loginStores USING(loginStoreId) WHERE loginStores.loginStoreType = 4 AND loginStores.loginStoreMode != 0;")

        // force full sync when updating from <4.6
        database.execSQL("DELETE FROM endpointTimers WHERE profileId IN (SELECT id FROM _90_ids);")
        database.execSQL("UPDATE profiles SET empty = 1 WHERE profileId IN (SELECT id FROM _90_ids);")
        // remove messages and events to re-download attachments and remove older than current school year
        database.execSQL("DELETE FROM messages WHERE profileId IN (SELECT id FROM _90_ids);")
        database.execSQL("DELETE FROM events WHERE profileId IN (SELECT id FROM _90_ids) AND eventAddedManually = 0;")
        // remove older data
        database.execSQL("DELETE FROM notices WHERE profileId IN (SELECT id FROM _90_ids);")

        // fix for v4.5 users who logged in using Vulcan/Web
        database.execSQL("UPDATE loginStores SET loginStoreMode = 2 WHERE loginStoreType = 4 AND loginStoreMode = 1;")

        database.execSQL("DROP TABLE _90_ids;")
    }
}
