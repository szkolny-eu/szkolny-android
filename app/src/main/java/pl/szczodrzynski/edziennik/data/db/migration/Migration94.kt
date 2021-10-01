/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-1.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore

class Migration94 : Migration(93, 94) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // events - is downloaded flag

        // get all profiles using Mobidziennik
        database.execSQL("CREATE TABLE _94_ids (id INTEGER NOT NULL);")
        database.execSQL("INSERT INTO _94_ids SELECT profileId FROM profiles JOIN loginStores USING(loginStoreId) WHERE loginStores.loginStoreType = ${LoginStore.LOGIN_TYPE_MOBIDZIENNIK};")

        database.execSQL("ALTER TABLE events ADD COLUMN eventIsDownloaded INT NOT NULL DEFAULT 1;")
        // set isDownloaded = 0 for information events in Mobidziennik
        database.execSQL("UPDATE events SET eventIsDownloaded = 0 WHERE profileId IN (SELECT id FROM _94_ids) AND eventType = ${Event.TYPE_INFORMATION};")

        database.execSQL("DROP TABLE _94_ids;")
    }
}
