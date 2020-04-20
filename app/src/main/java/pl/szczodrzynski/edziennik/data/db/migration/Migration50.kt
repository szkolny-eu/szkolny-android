/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration50 : Migration(49, 50) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE grades ADD gradeIsImprovement INTEGER NOT NULL DEFAULT 0")
        database.execSQL("DELETE FROM attendances WHERE attendances.profileId IN (SELECT profiles.profileId FROM profiles JOIN loginStores USING(loginStoreId) WHERE loginStores.loginStoreType = 1)")
        database.execSQL("UPDATE profiles SET empty = 1 WHERE profileId IN (SELECT profiles.profileId FROM profiles JOIN loginStores USING(loginStoreId) WHERE loginStores.loginStoreType = 4)")
    }
}
