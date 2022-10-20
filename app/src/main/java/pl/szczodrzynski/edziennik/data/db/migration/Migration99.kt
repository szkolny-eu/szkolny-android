/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-19.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration99 : Migration(98, 99) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // enum refactor, part 1 - make LoginStore modes unique, even without type
        database.execSQL("UPDATE loginStores SET loginStoreMode = loginStoreType * 100 + loginStoreMode;")
    }
}
