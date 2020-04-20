/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration16 : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE profiles (
                profileId INTEGER NOT NULL,
                name TEXT,
                subname TEXT,
                image TEXT,
                syncEnabled INTEGER NOT NULL,
                syncNotifications INTEGER NOT NULL,
                enableSharedEvents INTEGER NOT NULL,
                countInSeconds INTEGER NOT NULL,
                loggedIn INTEGER NOT NULL,
                empty INTEGER NOT NULL,
                studentNameLong TEXT,
                studentNameShort TEXT,
                studentNumber INTEGER NOT NULL,
                studentData TEXT,
                registration INTEGER NOT NULL,
                gradeColorMode INTEGER NOT NULL,
                agendaViewType INTEGER NOT NULL,
                currentSemester INTEGER NOT NULL,
                attendancePercentage REAL NOT NULL,
                dateSemester1Start TEXT,
                dateSemester2Start TEXT,
                dateYearEnd TEXT,
                luckyNumberEnabled INTEGER NOT NULL,
                luckyNumber INTEGER NOT NULL,
                luckyNumberDate TEXT,
                loginStoreId INTEGER NOT NULL,
                PRIMARY KEY(profileId));""")
    }
}
