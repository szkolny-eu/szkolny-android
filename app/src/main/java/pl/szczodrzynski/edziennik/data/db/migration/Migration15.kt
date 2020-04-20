/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration15 : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE grades RENAME TO _grades_old;")
        database.execSQL("CREATE TABLE `grades` (\n" +
                "\t`profileId`\tINTEGER NOT NULL,\n" +
                "\t`gradeId`\tINTEGER NOT NULL,\n" +
                "\t`gradeDescription`\tTEXT,\n" +
                "\t`gradeName`\tTEXT,\n" +
                "\t`gradeValue`\tREAL NOT NULL,\n" +
                "\t`gradeWeight`\tREAL NOT NULL,\n" +
                "\t`gradeSemester`\tINTEGER NOT NULL,\n" +
                "\t`gradeType`\tINTEGER NOT NULL,\n" +
                "\t`teacherId`\tINTEGER NOT NULL,\n" +
                "\t`categoryId`\tINTEGER NOT NULL,\n" +
                "\t`subjectId`\tINTEGER NOT NULL,\n" +
                "\tPRIMARY KEY(`profileId`,`gradeId`)\n" +
                ");")
        database.execSQL("INSERT INTO grades\n" +
                "   SELECT *\n" +
                "   FROM _grades_old;")
        database.execSQL("DROP TABLE _grades_old;")
        database.execSQL("CREATE INDEX index_grades_profileId ON grades (profileId);")
    }
}
