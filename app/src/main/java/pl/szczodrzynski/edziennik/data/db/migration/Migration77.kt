/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-19.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration77 : Migration(76, 77) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // mobidziennik web attendance implementation:
        // delete all attendance from mobidziennik profiles
        // (ID conflict/duplicated items - no ID in HTML of the website)
        database.execSQL("DELETE FROM attendances WHERE profileId IN (SELECT profileId FROM profiles WHERE loginStoreType = 1 AND archived = 0);")
        // mark the web attendance endpoint to force sync
        database.execSQL("DELETE FROM endpointTimers WHERE endpointId = 2050;")
    }
}
