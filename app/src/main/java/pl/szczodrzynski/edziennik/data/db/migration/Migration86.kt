/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-29.
 */

package pl.szczodrzynski.edziennik.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration86 : Migration(85, 86) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Migrating some models, moving addedDate from metadata to entities

        // Announcements
        database.execSQL("""ALTER TABLE announcements RENAME TO _announcements""")
        database.execSQL("""CREATE TABLE `announcements` (
            `announcementIdString` TEXT, 
            `profileId` INTEGER NOT NULL, 
            `announcementId` INTEGER NOT NULL, 
            `announcementSubject` TEXT NOT NULL, 
            `announcementText` TEXT, 
            `announcementStartDate` TEXT, 
            `announcementEndDate` TEXT, 
            `teacherId` INTEGER NOT NULL, 
            `addedDate` INTEGER NOT NULL, 
            `keep` INTEGER NOT NULL, 
            PRIMARY KEY(`profileId`, `announcementId`)
        )""")
        database.execSQL("""DROP INDEX IF EXISTS index_announcements_profileId""")
        database.execSQL("""CREATE INDEX `index_announcements_profileId` ON `announcements` (`profileId`)""")
        database.execSQL("""REPLACE INTO announcements (
            announcementIdString, profileId, announcementId, announcementSubject, announcementText, announcementStartDate, announcementEndDate, teacherId, addedDate, keep
        ) SELECT
            announcementIdString, profileId, announcementId, IFNULL(announcementSubject, ""), announcementText, announcementStartDate, announcementEndDate, teacherId, 0, 1 
        FROM _announcements""")
        database.execSQL("""DROP TABLE _announcements""")

        // Attendance Types
        database.execSQL("""ALTER TABLE attendanceTypes RENAME TO _attendanceTypes""")
        database.execSQL("""CREATE TABLE `attendanceTypes` (
            `profileId` INTEGER NOT NULL, 
            `id` INTEGER NOT NULL, 
            `baseType` INTEGER NOT NULL, 
            `typeName` TEXT NOT NULL, 
            `typeShort` TEXT NOT NULL, 
            `typeSymbol` TEXT NOT NULL, 
            `typeColor` INTEGER, 
            PRIMARY KEY(`profileId`, `id`)
        )""")
        database.execSQL("""REPLACE INTO attendanceTypes (
            profileId, id, 
            baseType, 
            typeName, 
            typeShort, 
            typeSymbol, 
            typeColor
        ) SELECT
            profileId, id, 
            CASE WHEN id > 100 AND type = 0 THEN 10 ELSE type END,
            name,
            CASE type WHEN 0 THEN "ob" WHEN 1 THEN "nb" WHEN 2 THEN "u" WHEN 3 THEN "zw" WHEN 4 THEN "sp" WHEN 5 THEN "su" WHEN 6 THEN "w" ELSE "?" END,
            CASE type WHEN 0 THEN "ob" WHEN 1 THEN "nb" WHEN 2 THEN "u" WHEN 3 THEN "zw" WHEN 4 THEN "sp" WHEN 5 THEN "su" WHEN 6 THEN "w" ELSE "?" END,
            CASE color WHEN -1 THEN NULL ELSE color END
        FROM _attendanceTypes""")
        database.execSQL("""DROP TABLE _attendanceTypes""")

        // Attendance
        database.execSQL("""ALTER TABLE attendances RENAME TO _attendances""")
        database.execSQL("""CREATE TABLE `attendances` (
            `attendanceLessonTopic` TEXT, 
            `attendanceLessonNumber` INTEGER, 
            `profileId` INTEGER NOT NULL, 
            `attendanceId` INTEGER NOT NULL, 
            `attendanceBaseType` INTEGER NOT NULL, 
            `attendanceTypeName` TEXT NOT NULL, 
            `attendanceTypeShort` TEXT NOT NULL, 
            `attendanceTypeSymbol` TEXT NOT NULL, 
            `attendanceTypeColor` INTEGER, 
            `attendanceDate` TEXT NOT NULL, 
            `attendanceTime` TEXT, 
            `attendanceSemester` INTEGER NOT NULL, 
            `teacherId` INTEGER NOT NULL, 
            `subjectId` INTEGER NOT NULL, 
            `addedDate` INTEGER NOT NULL, 
            `keep` INTEGER NOT NULL, 
            PRIMARY KEY(`profileId`, `attendanceId`)
        )""")
        database.execSQL("""DROP INDEX IF EXISTS index_attendances_profileId""")
        database.execSQL("""CREATE INDEX `index_attendances_profileId` ON `attendances` (`profileId`)""")
        database.execSQL("""REPLACE INTO attendances (
            attendanceLessonTopic, attendanceLessonNumber, profileId, attendanceId, 
            attendanceBaseType, 
            attendanceTypeName, 
            attendanceTypeShort, 
            attendanceTypeSymbol, 
            attendanceTypeColor, attendanceDate, attendanceTime, attendanceSemester, teacherId, subjectId, addedDate, keep
        ) SELECT
            attendanceLessonTopic, NULL, profileId, attendanceId, 
            attendanceType,
            CASE attendanceType WHEN 0 THEN "ob" WHEN 1 THEN "nb" WHEN 2 THEN "u" WHEN 3 THEN "zw" WHEN 4 THEN "sp" WHEN 5 THEN "su" WHEN 6 THEN "w" ELSE "?" END,
            CASE attendanceType WHEN 0 THEN "ob" WHEN 1 THEN "nb" WHEN 2 THEN "u" WHEN 3 THEN "zw" WHEN 4 THEN "sp" WHEN 5 THEN "su" WHEN 6 THEN "w" ELSE "?" END,
            CASE attendanceType WHEN 0 THEN "ob" WHEN 1 THEN "nb" WHEN 2 THEN "u" WHEN 3 THEN "zw" WHEN 4 THEN "sp" WHEN 5 THEN "su" WHEN 6 THEN "w" ELSE "?" END,
            NULL, attendanceLessonDate, attendanceStartTime, attendanceSemester, teacherId, subjectId, 0, 1
        FROM _attendances""")
        database.execSQL("""DROP TABLE _attendances""")

        // Events
        database.execSQL("""ALTER TABLE events ADD COLUMN addedDate INTEGER NOT NULL DEFAULT 0""")

        // Grades
        database.execSQL("""ALTER TABLE grades ADD COLUMN addedDate INTEGER NOT NULL DEFAULT 0""")
        database.execSQL("""ALTER TABLE grades ADD COLUMN keep INTEGER NOT NULL DEFAULT 1""")

        // Lucky Numbers
        database.execSQL("""ALTER TABLE luckyNumbers ADD COLUMN keep INTEGER NOT NULL DEFAULT 1""")

        // Messages
        database.execSQL("""ALTER TABLE messages ADD COLUMN addedDate INTEGER NOT NULL DEFAULT 0""")

        // Notices
        database.execSQL("""ALTER TABLE notices RENAME TO _notices""")
        database.execSQL("""CREATE TABLE `notices` (
            `profileId` INTEGER NOT NULL, 
            `noticeId` INTEGER NOT NULL, 
            `noticeType` INTEGER NOT NULL, 
            `noticeSemester` INTEGER NOT NULL, 
            `noticeText` TEXT NOT NULL, 
            `noticeCategory` TEXT, 
            `noticePoints` REAL, 
            `teacherId` INTEGER NOT NULL, 
            `addedDate` INTEGER NOT NULL, 
            `keep` INTEGER NOT NULL, 
            PRIMARY KEY(`profileId`, `noticeId`)
        )""")
        database.execSQL("""DROP INDEX IF EXISTS index_notices_profileId""")
        database.execSQL("""CREATE INDEX `index_notices_profileId` ON `notices` (`profileId`)""")
        database.execSQL("""REPLACE INTO notices (
            profileId, noticeId, noticeType, noticeSemester, 
            noticeText, 
            noticeCategory, 
            noticePoints, 
            teacherId, addedDate, keep
        ) SELECT
            profileId, noticeId, noticeType, noticeSemester, 
            CASE noticeText WHEN NULL THEN "" ELSE noticeText END, 
            category, 
            CASE points WHEN 0 THEN NULL ELSE points END, 
            teacherId, 0, 1
        FROM _notices""")
        database.execSQL("""DROP TABLE _notices""")

        // Teacher Absence
        database.execSQL("""ALTER TABLE teacherAbsence ADD COLUMN addedDate INTEGER NOT NULL DEFAULT 0""")
        database.execSQL("""ALTER TABLE teacherAbsence ADD COLUMN keep INTEGER NOT NULL DEFAULT 1""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_teacherAbsence_profileId` ON `teacherAbsence` (`profileId`)""")

        // Timetable
        database.execSQL("""ALTER TABLE timetable ADD COLUMN keep INTEGER NOT NULL DEFAULT 1""")

        // Metadata - copy AddedDate to entities
        database.execSQL("""UPDATE grades SET addedDate = IFNULL((SELECT metadata.addedDate FROM metadata WHERE metadata.profileId = grades.profileId AND metadata.thingId = grades.gradeId AND metadata.thingType = 1), 0)""")
        database.execSQL("""UPDATE notices SET addedDate = IFNULL((SELECT metadata.addedDate FROM metadata WHERE metadata.profileId = notices.profileId AND metadata.thingId = notices.noticeId AND metadata.thingType = 2), 0)""")
        database.execSQL("""UPDATE attendances SET addedDate = IFNULL((SELECT metadata.addedDate FROM metadata WHERE metadata.profileId = attendances.profileId AND metadata.thingId = attendances.attendanceId AND metadata.thingType = 3), 0)""")
        database.execSQL("""UPDATE events SET addedDate = IFNULL((SELECT metadata.addedDate FROM metadata WHERE metadata.profileId = events.profileId AND metadata.thingId = events.eventId AND metadata.thingType = 4), 0)""")
        database.execSQL("""UPDATE announcements SET addedDate = IFNULL((SELECT metadata.addedDate FROM metadata WHERE metadata.profileId = announcements.profileId AND metadata.thingId = announcements.announcementId AND metadata.thingType = 7), 0)""")
        database.execSQL("""UPDATE messages SET addedDate = IFNULL((SELECT metadata.addedDate FROM metadata WHERE metadata.profileId = messages.profileId AND metadata.thingId = messages.messageId AND metadata.thingType = 8), 0)""")

        // Metadata - drop AddedDate column
        database.execSQL("""ALTER TABLE metadata RENAME TO _metadata""")
        database.execSQL("""CREATE TABLE metadata (profileId INTEGER NOT NULL, metadataId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, thingType INTEGER NOT NULL, thingId INTEGER NOT NULL, seen INTEGER NOT NULL, notified INTEGER NOT NULL)""")
        database.execSQL("""DROP INDEX IF EXISTS index_metadata_profileId_thingType_thingId""")
        database.execSQL("""CREATE UNIQUE INDEX index_metadata_profileId_thingType_thingId ON "metadata" (profileId, thingType, thingId)""")
        database.execSQL("""INSERT INTO metadata (profileId, metadataId, thingType, thingId, seen, notified) SELECT profileId, metadataId, thingType, thingId, seen, notified FROM _metadata""")
        database.execSQL("""DROP TABLE _metadata""")
    }
}
