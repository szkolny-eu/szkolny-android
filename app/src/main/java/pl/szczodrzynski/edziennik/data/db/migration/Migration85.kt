package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_EDUDZIENNIK
import pl.szczodrzynski.edziennik.data.db.entity.Event

class Migration85 : Migration(84, 85) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM events WHERE eventAddedManually = 0 AND eventType = ${Event.TYPE_HOMEWORK} AND profileId IN (SELECT profileId FROM (SELECT profileId FROM profiles WHERE loginStoreType = $LOGIN_TYPE_EDUDZIENNIK) x)")
    }
}
