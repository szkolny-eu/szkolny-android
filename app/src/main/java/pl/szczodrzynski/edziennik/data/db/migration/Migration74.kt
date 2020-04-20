/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration74 : Migration(73, 74) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE librusLessons (
                profileId INTEGER NOT NULL,
                lessonId INTEGER NOT NULL,
                teacherId INTEGER NOT NULL,
                subjectId INTEGER NOT NULL,
                teamId INTEGER,
                PRIMARY KEY(profileId, lessonId))""")

        database.execSQL("CREATE INDEX index_librusLessons_profileId ON librusLessons (profileId)")
    }
}
