package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration77 : Migration(76, 77) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE lessonRanges")
        database.execSQL("""CREATE TABLE IF NOT EXISTS lessonRanges (
                profileId INTEGER NOT NULL,
                lessonRangeNumber INTEGER NOT NULL,
                lessonRangeStart TEXT NOT NULL,
                lessonRangeEnd TEXT NOT NULL,
                PRIMARY KEY(profileId, lessonRangeNumber))""")
    }
}
