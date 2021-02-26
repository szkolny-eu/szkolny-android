/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-26.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration91 : Migration(90, 91) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // get all profiles using Vulcan/Hebe
        database.execSQL("CREATE TABLE _91_ids (id INTEGER NOT NULL);")
        database.execSQL("INSERT INTO _91_ids SELECT profileId FROM profiles JOIN loginStores USING(loginStoreId) WHERE loginStores.loginStoreType = 1;")

        // force attendance sync - mobidziennik
        // after enabling counting the e-attendance to statistics
        database.execSQL("DELETE FROM endpointTimers WHERE profileId IN (SELECT id FROM _91_ids) AND endpointId = 2050;")
        database.execSQL("UPDATE attendances SET attendanceIsCounted = 1 WHERE profileId IN (SELECT id FROM _91_ids);")
        database.execSQL("UPDATE attendances SET attendanceBaseType = 2 WHERE profileId IN (SELECT id FROM _91_ids) AND attendanceTypeSymbol = ?;",
            arrayOf("+ₑ"))

        database.execSQL("DROP TABLE _91_ids;")
    }
}
