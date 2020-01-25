/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration64 : Migration(63, 64) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE timetable (
                profileId INTEGER NOT NULL,
                id INTEGER NOT NULL,
                type INTEGER NOT NULL,
                date TEXT DEFAULT NULL,
                lessonNumber INTEGER DEFAULT NULL,
                startTime TEXT DEFAULT NULL,
                endTime TEXT DEFAULT NULL,
                subjectId INTEGER DEFAULT NULL,
                teacherId INTEGER DEFAULT NULL,
                teamId INTEGER DEFAULT NULL,
                classroom TEXT DEFAULT NULL,
                oldDate TEXT DEFAULT NULL,
                oldLessonNumber INTEGER DEFAULT NULL,
                oldStartTime TEXT DEFAULT NULL,
                oldEndTime TEXT DEFAULT NULL,
                oldSubjectId INTEGER DEFAULT NULL,
                oldTeacherId INTEGER DEFAULT NULL,
                oldTeamId INTEGER DEFAULT NULL,
                oldClassroom TEXT DEFAULT NULL,
                PRIMARY KEY(id))""")

        database.execSQL("CREATE INDEX index_lessons_profileId_type_date ON timetable (profileId, type, date)")
        database.execSQL("CREATE INDEX index_lessons_profileId_type_oldDate ON timetable (profileId, type, oldDate)")
    }
}
