package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration79 : Migration(78, 79) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // manual timetable implementation
        database.execSQL("""CREATE TABLE timetableManual (
            profileId INTEGER NOT NULL,
            id INTEGER PRIMARY KEY NOT NULL,
            type INTEGER NOT NULL,
            repeatBy INTEGER NOT NULL DEFAULT 0,
            date INTEGER DEFAULT NULL,
            weekDay INTEGER DEFAULT NULL,
            lessonNumber INTEGER DEFAULT NULL,
            startTime TEXT DEFAULT NULL,
            endTime TEXT DEFAULT NULL,
            subjectId INTEGER DEFAULT NULL,
            teacherId INTEGER DEFAULT NULL,
            teamId INTEGER DEFAULT NULL,
            classroom TEXT DEFAULT NULL
        )""")
        database.execSQL("CREATE INDEX index_timetableManual_profileId_date ON timetableManual (profileId, date)")
        database.execSQL("CREATE INDEX index_timetableManual_profileId_weekDay ON timetableManual (profileId, weekDay)")
    }
}
