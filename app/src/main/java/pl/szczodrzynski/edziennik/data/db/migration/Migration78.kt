package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration78 : Migration(77, 78) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // grades migration to kotlin
        database.execSQL("ALTER TABLE grades RENAME TO _grades;")
        database.execSQL("""CREATE TABLE grades (
            profileId INTEGER NOT NULL,
            gradeId INTEGER NOT NULL,
            gradeName TEXT NOT NULL,
            gradeType INTEGER NOT NULL,
            gradeValue REAL NOT NULL,
            gradeWeight REAL NOT NULL,
            gradeColor INTEGER NOT NULL,
            gradeCategory TEXT,
            gradeDescription TEXT,
            gradeComment TEXT,
            gradeSemester INTEGER NOT NULL,
            teacherId INTEGER NOT NULL,
            subjectId INTEGER NOT NULL,
            gradeValueMax REAL DEFAULT NULL,
            gradeClassAverage REAL DEFAULT NULL,
            gradeParentId INTEGER DEFAULT NULL,
            gradeIsImprovement INTEGER NOT NULL,
            PRIMARY KEY(profileId, gradeId)
        );""")
        database.execSQL("DROP INDEX IF EXISTS index_grades_profileId;")
        database.execSQL("CREATE INDEX index_grades_profileId ON grades (profileId);")
        database.execSQL("""INSERT INTO grades (profileId, gradeId, gradeName, gradeType, gradeValue, gradeWeight, gradeColor, gradeCategory, gradeDescription, gradeComment, gradeSemester, teacherId, subjectId, gradeValueMax, gradeClassAverage, gradeParentId, gradeIsImprovement)
            SELECT profileId, gradeId, gradeName, gradeType, gradeValue, gradeWeight, gradeColor, 
            CASE gradeCategory WHEN '' THEN NULL WHEN ' ' THEN NULL ELSE gradeCategory END,
            CASE gradeDescription WHEN '' THEN NULL WHEN ' ' THEN NULL ELSE gradeDescription END,
            CASE gradeComment WHEN '' THEN NULL WHEN ' ' THEN NULL ELSE gradeComment END,
            gradeSemester, teacherId, subjectId,
            CASE gradeValueMax WHEN 0 THEN NULL ELSE gradeValueMax END,
            CASE gradeClassAverage WHEN -1 THEN NULL ELSE gradeClassAverage END,
            CASE gradeParentId WHEN -1 THEN NULL ELSE gradeParentId END,
            gradeIsImprovement FROM _grades;""")
        database.execSQL("DROP TABLE _grades;")
    }
}
