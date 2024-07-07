package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.data.enums.MetadataType

class Migration81 : Migration(80, 81) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE metadata SET seen = 1, notified = 1 WHERE thingType = ${MetadataType.TEACHER_ABSENCE.id}")
    }
}
