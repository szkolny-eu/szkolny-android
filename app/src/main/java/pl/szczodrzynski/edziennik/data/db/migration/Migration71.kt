/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.data.db.entity.Metadata.TYPE_MESSAGE

class Migration71 : Migration(70, 71) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM messages WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0)")
        database.execSQL("DELETE FROM messageRecipients WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0)")
        database.execSQL("DELETE FROM teachers WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0)")
        database.execSQL("DELETE FROM endpointTimers WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0)")
        database.execSQL("DELETE FROM metadata WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0) AND thingType = $TYPE_MESSAGE")
        database.execSQL("UPDATE profiles SET empty = 1 WHERE archived = 0")
        database.execSQL("UPDATE profiles SET lastReceiversSync = 0 WHERE archived = 0")
        database.execSQL("INSERT INTO config (profileId, `key`, value) VALUES (-1, 'runSync', 'true')")
    }
}
