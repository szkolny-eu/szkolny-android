/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration23 : Migration(22, 23) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE grades RENAME TO _grades_old;")
        database.execSQL("""CREATE TABLE `grades` (
                `profileId` INTEGER NOT NULL,
                `gradeId` INTEGER NOT NULL,
                `gradeCategory` TEXT,
                `gradeColor` INTEGER NOT NULL DEFAULT -1,
                `gradeDescription` TEXT,
                `gradeName` TEXT,
                `gradeValue` REAL NOT NULL,
                `gradeWeight` REAL NOT NULL,
                `gradeSemester` INTEGER NOT NULL,
                `gradeClassAverage` REAL NOT NULL DEFAULT -1,
                `gradeType` INTEGER NOT NULL,
                `teacherId` INTEGER NOT NULL,
                `subjectId` INTEGER NOT NULL,
                PRIMARY KEY(`profileId`,`gradeId`))""")

        database.execSQL("DROP INDEX index_grades_profileId")
        database.execSQL("CREATE INDEX `index_grades_profileId` ON `grades` (`profileId`)")

        database.execSQL("INSERT INTO grades (profileId, gradeId, gradeDescription, gradeName, gradeValue, gradeWeight, gradeSemester, gradeType, teacherId, subjectId, gradeClassAverage, gradeCategory, gradeColor) SELECT profileId, gradeId, gradeDescription, gradeName, gradeValue, gradeWeight, gradeSemester, gradeType, teacherId, subjectId, gradeClassAverage, gradeCategory, gradeColor FROM _grades_old;")
        database.execSQL("DROP TABLE _grades_old;")
        database.execSQL("ALTER TABLE attendances RENAME TO _attendances_old;")

        database.execSQL("""CREATE TABLE `attendances` (
                `profileId` INTEGER NOT NULL,
                `attendanceId` INTEGER NOT NULL,
                `attendanceLessonDate` TEXT NOT NULL,
                `attendanceStartTime` TEXT NOT NULL,
                `attendanceLessonTopic` TEXT,
                `attendanceSemester` INTEGER NOT NULL,
                `attendanceType` INTEGER NOT NULL,
                `teacherId` INTEGER NOT NULL,
                `subjectId` INTEGER NOT NULL,
                PRIMARY KEY(`profileId`,`attendanceId`,`attendanceLessonDate`,`attendanceStartTime`))""")

        database.execSQL("DROP INDEX index_attendances_profileId")
        database.execSQL("CREATE INDEX `index_attendances_profileId` ON `attendances` (`profileId`);")
        database.execSQL("INSERT INTO attendances SELECT * FROM _attendances_old;")
        database.execSQL("DROP TABLE _attendances_old;")
    }
}
