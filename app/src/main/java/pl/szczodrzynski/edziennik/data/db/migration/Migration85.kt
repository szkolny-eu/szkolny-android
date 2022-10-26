package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.enums.LoginType

class Migration85 : Migration(84, 85) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM events WHERE eventAddedManually = 0 AND eventType = ${Event.TYPE_HOMEWORK} AND profileId IN (SELECT profileId FROM (SELECT profileId FROM profiles WHERE loginStoreType = ${LoginType.EDUDZIENNIK.id}) x)")
    }
}
