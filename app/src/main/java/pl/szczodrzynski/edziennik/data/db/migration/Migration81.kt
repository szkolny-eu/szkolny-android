package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.data.db.entity.Metadata

class Migration81 : Migration(80, 81) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE metadata SET seen = 1, notified = 1 WHERE thingType = ${Metadata.TYPE_TEACHER_ABSENCE}")
    }
}
