package pl.szczodrzynski.edziennik.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.szczodrzynski.edziennik.config.db.ConfigDao
import pl.szczodrzynski.edziennik.config.db.ConfigEntry
import pl.szczodrzynski.edziennik.crc32
import pl.szczodrzynski.edziennik.data.db.converter.*
import pl.szczodrzynski.edziennik.data.db.dao.*
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.utils.models.Date

@Database(entities = [
    Grade::class,
    Teacher::class,
    TeacherAbsence::class,
    TeacherAbsenceType::class,
    Subject::class,
    Notice::class,
    Team::class,
    Attendance::class,
    Event::class,
    EventType::class,
    LoginStore::class,
    Profile::class,
    LuckyNumber::class,
    Announcement::class,
    GradeCategory::class,
    FeedbackMessage::class,
    Message::class,
    MessageRecipient::class,
    DebugLog::class,
    EndpointTimer::class,
    LessonRange::class,
    Notification::class,
    Classroom::class,
    NoticeType::class,
    AttendanceType::class,
    Lesson::class,
    ConfigEntry::class,
    LibrusLesson::class,
    Metadata::class
], version = 75)
@TypeConverters(
        ConverterTime::class,
        ConverterDate::class,
        ConverterJsonObject::class,
        ConverterListLong::class,
        ConverterListString::class,
        ConverterDateInt::class
)
abstract class AppDb : RoomDatabase() {
    abstract fun gradeDao(): GradeDao
    abstract fun teacherDao(): TeacherDao
    abstract fun teacherAbsenceDao(): TeacherAbsenceDao
    abstract fun teacherAbsenceTypeDao(): TeacherAbsenceTypeDao
    abstract fun subjectDao(): SubjectDao
    abstract fun noticeDao(): NoticeDao
    abstract fun teamDao(): TeamDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun eventDao(): EventDao
    abstract fun eventTypeDao(): EventTypeDao
    abstract fun loginStoreDao(): LoginStoreDao
    abstract fun profileDao(): ProfileDao
    abstract fun luckyNumberDao(): LuckyNumberDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun gradeCategoryDao(): GradeCategoryDao
    abstract fun feedbackMessageDao(): FeedbackMessageDao
    abstract fun messageDao(): MessageDao
    abstract fun messageRecipientDao(): MessageRecipientDao
    abstract fun debugLogDao(): DebugLogDao
    abstract fun endpointTimerDao(): EndpointTimerDao
    abstract fun lessonRangeDao(): LessonRangeDao
    abstract fun notificationDao(): NotificationDao
    abstract fun classroomDao(): ClassroomDao
    abstract fun noticeTypeDao(): NoticeTypeDao
    abstract fun attendanceTypeDao(): AttendanceTypeDao
    abstract fun timetableDao(): TimetableDao
    abstract fun configDao(): ConfigDao
    abstract fun librusLessonDao(): LibrusLessonDao
    abstract fun metadataDao(): MetadataDao

    companion object {
        @Volatile private var instance: AppDb? = null
        private val LOCK = Any()

        private val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys=off;")
                database.execSQL("ALTER TABLE teams RENAME TO _teams_old;")
                database.execSQL("CREATE TABLE teams (profileId INTEGER NOT NULL, teamId INTEGER NOT NULL, teamType INTEGER NOT NULL, teamName TEXT, teamTeacherId INTEGER NOT NULL, PRIMARY KEY(profileId, teamId));")
                database.execSQL("INSERT INTO teams (profileId, teamId, teamType, teamName, teamTeacherId) SELECT profileId, teamId, teamType, teamName, teacherId FROM _teams_old;")
                database.execSQL("DROP TABLE _teams_old;")
                database.execSQL("PRAGMA foreign_keys=on;")
            }
        }
        private val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lessonChanges ADD lessonChangeWeekDay INTEGER NOT NULL DEFAULT -1;")
            }
        }
        private val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE loginStores (loginStoreId INTEGER NOT NULL, loginStoreType INTEGER NOT NULL, loginStoreData TEXT, PRIMARY KEY(loginStoreId));")
            }
        }
        private val MIGRATION_14_15: Migration = object : Migration(14, 15) {
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
        private val MIGRATION_15_16: Migration = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE profiles (" +
                        "profileId INTEGER NOT NULL, " +
                        "name TEXT, " +
                        "subname TEXT, " +
                        "image TEXT, " +
                        "syncEnabled INTEGER NOT NULL, " +
                        "syncNotifications INTEGER NOT NULL, " +
                        "enableSharedEvents INTEGER NOT NULL, " +
                        "countInSeconds INTEGER NOT NULL, " +
                        "loggedIn INTEGER NOT NULL, " +
                        "empty INTEGER NOT NULL, " +
                        "studentNameLong TEXT, " +
                        "studentNameShort TEXT, " +
                        "studentNumber INTEGER NOT NULL, " +
                        "studentData TEXT, " +
                        "registration INTEGER NOT NULL, " +
                        "gradeColorMode INTEGER NOT NULL, " +
                        "agendaViewType INTEGER NOT NULL, " +
                        "currentSemester INTEGER NOT NULL, " +
                        "attendancePercentage REAL NOT NULL, " +
                        "dateSemester1Start TEXT, " +
                        "dateSemester2Start TEXT, " +
                        "dateYearEnd TEXT, " +
                        "luckyNumberEnabled INTEGER NOT NULL, " +
                        "luckyNumber INTEGER NOT NULL, " +
                        "luckyNumberDate TEXT, " +
                        "loginStoreId INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId));")
            }
        }
        private val MIGRATION_16_17: Migration = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profiles ADD archived INTEGER NOT NULL DEFAULT 0;")
                database.execSQL("ALTER TABLE teams ADD teamCode TEXT;")
            }
        }
        private val MIGRATION_17_18: Migration = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE events ADD eventBlacklisted INTEGER NOT NULL DEFAULT 0;")
            }
        }
        private val MIGRATION_18_19: Migration = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE grades ADD gradeClassAverage REAL NOT NULL DEFAULT -1;")
            }
        }
        private val MIGRATION_19_20: Migration = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE luckyNumbers (" +
                        "profileId INTEGER NOT NULL, " +
                        "luckyNumberDate TEXT NOT NULL, " +
                        "luckyNumber INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId, luckyNumberDate));")
            }
        }
        private val MIGRATION_20_21: Migration = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE grades ADD gradeCategory TEXT")
                database.execSQL("ALTER TABLE grades ADD gradeColor INTEGER NOT NULL DEFAULT -1")
                database.execSQL("DROP TABLE gradeCategories")
            }
        }
        private val MIGRATION_21_22: Migration = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE eventTypes (" +
                        "profileId INTEGER NOT NULL, " +
                        "eventType INTEGER NOT NULL, " +
                        "eventTypeName TEXT, " +
                        "eventTypeColor INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId, eventType));")
            }
        }
        private val MIGRATION_22_23: Migration = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE grades RENAME TO _grades_old;")
                database.execSQL("CREATE TABLE `grades` (\n" +
                        "\t`profileId`\tINTEGER NOT NULL,\n" +
                        "\t`gradeId`\tINTEGER NOT NULL,\n" +
                        "\t`gradeCategory`\tTEXT,\n" +
                        "\t`gradeColor`\tINTEGER NOT NULL DEFAULT -1,\n" +
                        "\t`gradeDescription`\tTEXT,\n" +
                        "\t`gradeName`\tTEXT,\n" +
                        "\t`gradeValue`\tREAL NOT NULL,\n" +
                        "\t`gradeWeight`\tREAL NOT NULL,\n" +
                        "\t`gradeSemester`\tINTEGER NOT NULL,\n" +
                        "\t`gradeClassAverage`\tREAL NOT NULL DEFAULT -1,\n" +
                        "\t`gradeType`\tINTEGER NOT NULL,\n" +
                        "\t`teacherId`\tINTEGER NOT NULL,\n" +
                        "\t`subjectId`\tINTEGER NOT NULL,\n" +
                        "\tPRIMARY KEY(`profileId`,`gradeId`)\n" +
                        ");")
                database.execSQL("DROP INDEX index_grades_profileId")
                database.execSQL("CREATE INDEX `index_grades_profileId` ON `grades` (\n" +
                        "\t`profileId`\n" +
                        ");")
                database.execSQL("INSERT INTO grades (profileId, gradeId, gradeDescription, gradeName, gradeValue, gradeWeight, gradeSemester, gradeType, teacherId, subjectId, gradeClassAverage, gradeCategory, gradeColor) SELECT profileId, gradeId, gradeDescription, gradeName, gradeValue, gradeWeight, gradeSemester, gradeType, teacherId, subjectId, gradeClassAverage, gradeCategory, gradeColor FROM _grades_old;")
                database.execSQL("DROP TABLE _grades_old;")
                database.execSQL("ALTER TABLE attendances RENAME TO _attendances_old;")
                database.execSQL("CREATE TABLE `attendances` (\n" +
                        "\t`profileId`\tINTEGER NOT NULL,\n" +
                        "\t`attendanceId`\tINTEGER NOT NULL,\n" +
                        "\t`attendanceLessonDate`\tTEXT NOT NULL,\n" +
                        "\t`attendanceStartTime`\tTEXT NOT NULL,\n" +
                        "\t`attendanceLessonTopic`\tTEXT,\n" +
                        "\t`attendanceSemester`\tINTEGER NOT NULL,\n" +
                        "\t`attendanceType`\tINTEGER NOT NULL,\n" +
                        "\t`teacherId`\tINTEGER NOT NULL,\n" +
                        "\t`subjectId`\tINTEGER NOT NULL,\n" +
                        "\tPRIMARY KEY(`profileId`,`attendanceId`,`attendanceLessonDate`,`attendanceStartTime`)\n" +
                        ");")
                database.execSQL("DROP INDEX index_attendances_profileId")
                database.execSQL("CREATE INDEX `index_attendances_profileId` ON `attendances` (\n" +
                        "\t`profileId`\n" +
                        ");")
                database.execSQL("INSERT INTO attendances SELECT * FROM _attendances_old;")
                database.execSQL("DROP TABLE _attendances_old;")
            }
        }
        private val MIGRATION_23_24: Migration = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profiles ADD yearAverageMode INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_24_25: Migration = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE announcements (" +
                        "profileId INTEGER NOT NULL, " +
                        "announcementId INTEGER NOT NULL, " +
                        "announcementSubject TEXT, " +
                        "announcementText TEXT, " +
                        "announcementStartDate TEXT, " +
                        "announcementEndDate TEXT, " +
                        "teacherId INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId, announcementId));")
                //database.execSQL("DROP INDEX index_announcements_profileId");
                database.execSQL("CREATE INDEX index_announcements_profileId ON announcements (" +
                        "profileId" +
                        ");")
            }
        }
        private val MIGRATION_25_26: Migration = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE grades ADD gradePointGrade INTEGER NOT NULL DEFAULT 0;")
            }
        }
        private val MIGRATION_26_27: Migration = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE grades ADD gradeValueMax REAL NOT NULL DEFAULT 0;")
            }
        }
        private val MIGRATION_27_28: Migration = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE gradeCategories (profileId INTEGER NOT NULL, categoryId INTEGER NOT NULL, weight REAL NOT NULL, color INTEGER NOT NULL, `text` TEXT, columns TEXT, valueFrom REAL NOT NULL, valueTo REAL NOT NULL, PRIMARY KEY(profileId, categoryId));")
                database.execSQL("ALTER TABLE grades ADD gradeComment TEXT;")
            }
        }
        private val MIGRATION_28_29: Migration = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE feedbackMessages (messageId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, received INTEGER NOT NULL DEFAULT 0, sentTime INTEGER NOT NULL, `text` TEXT)")
            }
        }
        private val MIGRATION_29_30: Migration = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE feedbackMessages ADD fromUser TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE feedbackMessages ADD fromUserName TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_30_31: Migration = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE messages (" +
                        "profileId INTEGER NOT NULL, " +
                        "messageId INTEGER NOT NULL, " +
                        "messageSubject TEXT, " +
                        "messageBody TEXT DEFAULT NULL, " +
                        "messageType INTEGER NOT NULL DEFAULT 0, " +
                        "senderId INTEGER NOT NULL DEFAULT -1, " +
                        "senderReplyId INTEGER NOT NULL DEFAULT -1, " +
                        "recipientIds TEXT DEFAULT NULL, " +
                        "recipientReplyIds TEXT DEFAULT NULL, " +
                        "readByRecipientDates TEXT DEFAULT NULL, " +
                        "overrideHasAttachments INTEGER NOT NULL DEFAULT 0, " +
                        "attachmentIds TEXT DEFAULT NULL, " +
                        "attachmentNames TEXT DEFAULT NULL, " +
                        "PRIMARY KEY(profileId, messageId));")
                //database.execSQL("DROP INDEX index_announcements_profileId");
                database.execSQL("CREATE INDEX index_messages_profileId ON messages (" +
                        "profileId" +
                        ");")
            }
        }
        private val MIGRATION_31_32: Migration = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD attachmentSizes TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_32_33: Migration = object : Migration(32, 33) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE messages")
                database.execSQL("CREATE TABLE messages (" +
                        "profileId INTEGER NOT NULL, " +
                        "messageId INTEGER NOT NULL, " +
                        "messageSubject TEXT, " +
                        "messageBody TEXT DEFAULT NULL, " +
                        "messageType INTEGER NOT NULL DEFAULT 0, " +
                        "senderId INTEGER NOT NULL DEFAULT -1, " +
                        "senderReplyId INTEGER NOT NULL DEFAULT -1, " +
                        "overrideHasAttachments INTEGER NOT NULL DEFAULT 0, " +
                        "attachmentIds TEXT DEFAULT NULL, " +
                        "attachmentNames TEXT DEFAULT NULL, " +
                        "attachmentSizes TEXT DEFAULT NULL, " +
                        "PRIMARY KEY(profileId, messageId));")
                //database.execSQL("DROP INDEX index_announcements_profileId");
                database.execSQL("CREATE INDEX index_messages_profileId ON messages (" +
                        "profileId" +
                        ");")
                database.execSQL("CREATE TABLE messageRecipients (" +
                        "profileId INTEGER NOT NULL, " +
                        "messageRecipientId INTEGER NOT NULL DEFAULT -1, " +
                        "messageRecipientReplyId INTEGER NOT NULL DEFAULT -1, " +
                        "messageRecipientReadDate INTEGER NOT NULL DEFAULT -1, " +
                        "messageId INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId, messageRecipientId));")
            }
        }
        private val MIGRATION_33_34: Migration = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE messageRecipients")
                database.execSQL("CREATE TABLE messageRecipients (" +
                        "profileId INTEGER NOT NULL, " +
                        "messageRecipientId INTEGER NOT NULL DEFAULT -1, " +
                        "messageRecipientReplyId INTEGER NOT NULL DEFAULT -1, " +
                        "messageRecipientReadDate INTEGER NOT NULL DEFAULT -1, " +
                        "messageId INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId, messageRecipientId, messageId));")
            }
        }
        private val MIGRATION_34_35: Migration = object : Migration(34, 35) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE teachers ADD teacherLoginId TEXT DEFAULT NULL;")
            }
        }
        private val MIGRATION_35_36: Migration = object : Migration(35, 36) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE profiles SET yearAverageMode = 4 WHERE yearAverageMode = 0")
            }
        }
        private val MIGRATION_36_37: Migration = object : Migration(36, 37) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }
        private val MIGRATION_37_38: Migration = object : Migration(37, 38) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val today = Date.getToday()
                val schoolYearStart = if (today.month < 9) today.year - 1 else today.year
                database.execSQL("UPDATE profiles SET dateSemester1Start = '$schoolYearStart-09-01' WHERE dateSemester1Start IS NULL;")
                database.execSQL("UPDATE profiles SET dateSemester2Start = '" + (schoolYearStart + 1) + "-02-01' WHERE dateSemester2Start IS NULL;")
                database.execSQL("UPDATE profiles SET dateYearEnd = '" + (schoolYearStart + 1) + "-06-30' WHERE dateYearEnd IS NULL;")
            }
        }
        private val MIGRATION_38_39: Migration = object : Migration(38, 39) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE debugLogs (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `text` TEXT);")
            }
        }
        private val MIGRATION_39_40: Migration = object : Migration(39, 40) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profiles ADD changedEndpoints TEXT DEFAULT NULL;")
            }
        }
        private val MIGRATION_40_41: Migration = object : Migration(40, 41) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE profiles SET empty = 1;")
            }
        }
        private val MIGRATION_41_42: Migration = object : Migration(41, 42) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE profiles SET empty = 1;")
            }
        }
        private val MIGRATION_42_43: Migration = object : Migration(42, 43) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profiles ADD lastFullSync INTEGER NOT NULL DEFAULT 0;")
            }
        }
        private val MIGRATION_43_44: Migration = object : Migration(43, 44) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE profiles SET empty = 1;")
                database.execSQL("UPDATE profiles SET currentSemester = 1;")
            }
        }
        private val MIGRATION_44_45: Migration = object : Migration(44, 45) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE profiles SET empty = 1;")
            }
        }
        private val MIGRATION_45_46: Migration = object : Migration(45, 46) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE profiles SET lastFullSync = 0")
            }
        }
        private val MIGRATION_46_47: Migration = object : Migration(46, 47) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE profiles SET lastFullSync = 0")
            }
        }
        private val MIGRATION_47_48: Migration = object : Migration(47, 48) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notices ADD points REAL NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notices ADD category TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_48_49: Migration = object : Migration(48, 49) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE grades ADD gradeParentId INTEGER NOT NULL DEFAULT -1")
            }
        }
        private val MIGRATION_49_50: Migration = object : Migration(49, 50) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE grades ADD gradeIsImprovement INTEGER NOT NULL DEFAULT 0")
                database.execSQL("DELETE FROM attendances WHERE attendances.profileId IN (SELECT profiles.profileId FROM profiles JOIN loginStores USING(loginStoreId) WHERE loginStores.loginStoreType = 1)")
                database.execSQL("UPDATE profiles SET empty = 1 WHERE profileId IN (SELECT profiles.profileId FROM profiles JOIN loginStores USING(loginStoreId) WHERE loginStores.loginStoreType = 4)")
            }
        }
        private val MIGRATION_50_51: Migration = object : Migration(50, 51) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profiles ADD lastReceiversSync INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE teachers ADD teacherType INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_51_52: Migration = object : Migration(51, 52) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE teachers ADD teacherTypeDescription TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_52_53: Migration = object : Migration(52, 53) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS teacherAbsence (" +
                        "profileId INTEGER NOT NULL," +
                        "teacherAbsenceId INTEGER NOT NULL," +
                        "teacherId INTEGER NOT NULL," +
                        "teacherAbsenceType INTEGER NOT NULL," +
                        "teacherAbsenceDateFrom TEXT NOT NULL," +
                        "teacherAbsenceDateTo TEXT NOT NULL," +
                        "PRIMARY KEY(profileId, teacherAbsenceId)" +
                        ")")
            }
        }
        private val MIGRATION_53_54: Migration = object : Migration(53, 54) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE teacherAbsence ADD teacherAbsenceTimeFrom TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE teacherAbsence ADD teacherAbsenceTimeTo TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_54_55: Migration = object : Migration(54, 55) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 2019-10-21 for merge compatibility between 3.1.1 and api-v2
                // moved to Migration 55->56
            }
        }
        private val MIGRATION_55_56: Migration = object : Migration(55, 56) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS endpointTimers (" +
                        "profileId INTEGER NOT NULL," +
                        "endpointId INTEGER NOT NULL," +
                        "endpointLastSync INTEGER DEFAULT NULL," +
                        "endpointNextSync INTEGER NOT NULL DEFAULT 1," +
                        "endpointViewId INTEGER DEFAULT NULL," +
                        "PRIMARY KEY(profileId, endpointId)" +
                        ")")
                database.execSQL("CREATE TABLE IF NOT EXISTS lessonRanges (" +
                        "profileId INTEGER NOT NULL," +
                        "lessonRangeNumber INTEGER NOT NULL," +
                        "lessonRangeStart TEXT NOT NULL," +
                        "lessonRangeEnd TEXT NOT NULL," +
                        "PRIMARY KEY(profileId, lessonRangeNumber)" +
                        ")")
            }
        }
        private val MIGRATION_56_57: Migration = object : Migration(56, 57) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE gradeCategories ADD type INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_57_58: Migration = object : Migration(57, 58) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE metadata RENAME TO _metadata_old;")
                database.execSQL("DROP INDEX index_metadata_profileId_thingType_thingId;")
                database.execSQL("UPDATE _metadata_old SET thingType = " + Metadata.TYPE_HOMEWORK + " WHERE thingType = " + Metadata.TYPE_EVENT + " AND thingId IN (SELECT eventId FROM events WHERE eventType = -1);")
                database.execSQL("CREATE TABLE metadata (\n" +
                        "profileId INTEGER NOT NULL,\n" +
                        "metadataId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                        "thingType INTEGER NOT NULL,\n" +
                        "thingId INTEGER NOT NULL,\n" +
                        "seen INTEGER NOT NULL,\n" +
                        "notified INTEGER NOT NULL,\n" +
                        "addedDate INTEGER NOT NULL);")
                database.execSQL("INSERT INTO metadata SELECT * FROM (SELECT * FROM _metadata_old ORDER BY addedDate DESC) GROUP BY thingId;")
                database.execSQL("DROP TABLE _metadata_old;")
                database.execSQL("CREATE UNIQUE INDEX index_metadata_profileId_thingType_thingId ON metadata (profileId, thingType, thingId);")
            }
        }
        private val MIGRATION_58_59: Migration = object : Migration(58, 59) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE metadata SET addedDate = addedDate*1000 WHERE addedDate < 10000000000;")
                database.execSQL("INSERT INTO metadata (profileId, thingType, thingId, seen, notified, addedDate)\n" +
                        "SELECT profileId,\n" +
                        "10 AS thingType,\n" +
                        "luckyNumberDate*10000+substr(luckyNumberDate, 6)*100+substr(luckyNumberDate, 9) AS thingId,\n" +
                        "1 AS seen,\n" +
                        "1 AS notified,\n" +
                        "CAST(strftime('%s', luckyNumberDate) AS INT)*1000 AS addedDate\n" +
                        "FROM luckyNumbers;")
                database.execSQL("ALTER TABLE luckyNumbers RENAME TO _old_luckyNumbers;")
                database.execSQL("CREATE TABLE luckyNumbers (\n" +
                        "profileId INTEGER NOT NULL,\n" +
                        "luckyNumberDate INTEGER NOT NULL, \n" +
                        "luckyNumber INTEGER NOT NULL, \n" +
                        "PRIMARY KEY(profileId, luckyNumberDate))")
                database.execSQL("INSERT INTO luckyNumbers (profileId, luckyNumberDate, luckyNumber)\n" +
                        "SELECT profileId,\n" +
                        "luckyNumberDate*10000+substr(luckyNumberDate, 6)*100+substr(luckyNumberDate, 9) AS luckyNumberDate,\n" +
                        "luckyNumber\n" +
                        "FROM _old_luckyNumbers;")
                database.execSQL("DROP TABLE _old_luckyNumbers;")
            }
        }
        private val MIGRATION_59_60: Migration = object : Migration(59, 60) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE notifications (\n" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                        "    title TEXT NOT NULL,\n" +
                        "    `text` TEXT NOT NULL,\n" +
                        "    `type` INTEGER NOT NULL,\n" +
                        "    profileId INTEGER DEFAULT NULL,\n" +
                        "    profileName TEXT DEFAULT NULL,\n" +
                        "    posted INTEGER NOT NULL DEFAULT 0,\n" +
                        "    viewId INTEGER DEFAULT NULL,\n" +
                        "    extras TEXT DEFAULT NULL,\n" +
                        "    addedDate INTEGER NOT NULL\n" +
                        ");")
                database.execSQL("ALTER TABLE profiles ADD COLUMN disabledNotifications TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_60_61: Migration = object : Migration(60, 61) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS teacherAbsenceTypes (" +
                        "profileId INTEGER NOT NULL," +
                        "teacherAbsenceTypeId INTEGER NOT NULL," +
                        "teacherAbsenceTypeName TEXT NOT NULL," +
                        "PRIMARY KEY(profileId, teacherAbsenceTypeId))")
                database.execSQL("ALTER TABLE teacherAbsence ADD COLUMN teacherAbsenceName TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_61_62: Migration = object : Migration(61, 62) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS classrooms (\n" +
                        "    profileId INTEGER NOT NULL,\n" +
                        "    id INTEGER NOT NULL,\n" +
                        "    name TEXT NOT NULL,\n" +
                        "    PRIMARY KEY(profileId, id)\n" +
                        ")")
                database.execSQL("CREATE TABLE IF NOT EXISTS noticeTypes (\n" +
                        "    profileId INTEGER NOT NULL,\n" +
                        "    id INTEGER NOT NULL,\n" +
                        "    name TEXT NOT NULL,\n" +
                        "    PRIMARY KEY(profileId, id)\n" +
                        ")")
                database.execSQL("CREATE TABLE IF NOT EXISTS attendanceTypes (\n" +
                        "    profileId INTEGER NOT NULL,\n" +
                        "    id INTEGER NOT NULL,\n" +
                        "    name TEXT NOT NULL,\n" +
                        "    type INTEGER NOT NULL,\n" +
                        "    color INTEGER NOT NULL,\n" +
                        "    PRIMARY KEY(profileId, id)\n" +
                        ")")
            }
        }
        private val MIGRATION_62_63: Migration = object : Migration(62, 63) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profiles ADD COLUMN accountNameLong TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE profiles ADD COLUMN studentClassName TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE profiles ADD COLUMN studentSchoolYear TEXT DEFAULT NULL")
            }
        }
        private val MIGRATION_63_64: Migration = object : Migration(63, 64) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //database.execSQL("ALTER TABLE lessons RENAME TO lessonsOld;");
                database.execSQL("CREATE TABLE timetable (" +
                        "profileId INTEGER NOT NULL," +
                        "id INTEGER NOT NULL," +
                        "type INTEGER NOT NULL," +
                        "date TEXT DEFAULT NULL," +
                        "lessonNumber INTEGER DEFAULT NULL," +
                        "startTime TEXT DEFAULT NULL," +
                        "endTime TEXT DEFAULT NULL," +
                        "subjectId INTEGER DEFAULT NULL," +
                        "teacherId INTEGER DEFAULT NULL," +
                        "teamId INTEGER DEFAULT NULL," +
                        "classroom TEXT DEFAULT NULL," +
                        "oldDate TEXT DEFAULT NULL," +
                        "oldLessonNumber INTEGER DEFAULT NULL," +
                        "oldStartTime TEXT DEFAULT NULL," +
                        "oldEndTime TEXT DEFAULT NULL," +
                        "oldSubjectId INTEGER DEFAULT NULL," +
                        "oldTeacherId INTEGER DEFAULT NULL," +
                        "oldTeamId INTEGER DEFAULT NULL," +
                        "oldClassroom TEXT DEFAULT NULL," +
                        "PRIMARY KEY(id));")
                database.execSQL("CREATE INDEX index_lessons_profileId_type_date ON timetable (profileId, type, date);")
                database.execSQL("CREATE INDEX index_lessons_profileId_type_oldDate ON timetable (profileId, type, oldDate);")
            }
        }
        private val MIGRATION_64_65: Migration = object : Migration(64, 65) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE config (" +
                        "profileId INTEGER NOT NULL DEFAULT -1," +
                        "`key` TEXT NOT NULL," +
                        "value TEXT NOT NULL," +
                        "PRIMARY KEY(profileId, `key`));")
            }
        }
        private val MIGRATION_65_66: Migration = object : Migration(65, 66) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE config;")
                database.execSQL("CREATE TABLE config (" +
                        "profileId INTEGER NOT NULL DEFAULT -1," +
                        "`key` TEXT NOT NULL," +
                        "value TEXT," +
                        "PRIMARY KEY(profileId, `key`));")
            }
        }
        private val MIGRATION_66_67: Migration = object : Migration(66, 67) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE FROM grades WHERE (gradeId=-1 OR gradeId=-2) AND gradeType=20")
                database.execSQL("DELETE FROM metadata WHERE (thingId=-1 OR thingId=-2) AND thingType=1")
                database.execSQL("ALTER TABLE gradeCategories RENAME TO _gradeCategories")
                database.execSQL("CREATE TABLE gradeCategories (" +
                        "profileId INTEGER NOT NULL," +
                        "categoryId INTEGER NOT NULL," +
                        "weight REAL NOT NULL," +
                        "color INTEGER NOT NULL," +
                        "`text` TEXT," +
                        "columns TEXT," +
                        "valueFrom REAL NOT NULL," +
                        "valueTo REAL NOT NULL," +
                        "type INTEGER NOT NULL," +
                        "PRIMARY KEY(profileId, categoryId, type))")
                database.execSQL("INSERT INTO gradeCategories (profileId, categoryId, weight, color," +
                        "`text`, columns, valueFrom, valueTo, type) " +
                        "SELECT profileId, categoryId, weight, color, `text`, columns, valueFrom," +
                        "valueTo, type FROM _gradeCategories")
                database.execSQL("DROP TABLE _gradeCategories")
            }
        }
        private val MIGRATION_67_68: Migration = object : Migration(67, 68) {
            override fun migrate(database: SupportSQLiteDatabase) {
                /* Migration from crc16 to crc32 id */
                database.execSQL("DELETE FROM announcements")
                database.execSQL("DELETE FROM metadata WHERE thingType=7")
            }
        }
        private val MIGRATION_68_69: Migration = object : Migration(68, 69) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE loginStores ADD COLUMN loginStoreMode INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_69_70: Migration = object : Migration(69, 70) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE announcements ADD COLUMN announcementIdString TEXT DEFAULT NULL")
                database.execSQL("DELETE FROM announcements")
            }
        }
        private val MIGRATION_70_71: Migration = object : Migration(70, 71) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE FROM messages WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);")
                database.execSQL("DELETE FROM messageRecipients WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);")
                database.execSQL("DELETE FROM teachers WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);")
                database.execSQL("DELETE FROM endpointTimers WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);")
                database.execSQL("DELETE FROM metadata WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0) AND thingType = 8;")
                database.execSQL("UPDATE profiles SET empty = 1 WHERE archived = 0;")
                database.execSQL("UPDATE profiles SET lastReceiversSync = 0 WHERE archived = 0;")
                database.execSQL("INSERT INTO config (profileId, `key`, value) VALUES (-1, \"runSync\", \"true\");")
            }
        }
        private val MIGRATION_71_72: Migration = object : Migration(71, 72) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE loginStores RENAME to _loginStores;")
                database.execSQL("CREATE TABLE loginStores(" +
                        "loginStoreId INTEGER NOT NULL," +
                        "loginStoreType INTEGER NOT NULL," +
                        "loginStoreMode INTEGER NOT NULL," +
                        "loginStoreData TEXT NOT NULL," +
                        "PRIMARY KEY(loginStoreId));")
                database.execSQL("INSERT INTO loginStores " +
                        "(loginStoreId, loginStoreType, loginStoreMode, loginStoreData) " +
                        "SELECT loginStoreId, loginStoreType, loginStoreMode, loginStoreData " +
                        "FROM _loginStores;")
                database.execSQL("DROP TABLE _loginStores;")
                database.execSQL("ALTER TABLE profiles RENAME TO _profiles_old;")
                database.execSQL("CREATE TABLE profiles (\n" +
                        "profileId INTEGER NOT NULL, name TEXT NOT NULL, subname TEXT, image TEXT DEFAULT NULL, \n" +
                        "studentNameLong TEXT NOT NULL, studentNameShort TEXT NOT NULL, accountName TEXT, \n" +
                        "studentData TEXT NOT NULL, empty INTEGER NOT NULL DEFAULT 1, archived INTEGER NOT NULL DEFAULT 0, \n" +
                        "syncEnabled INTEGER NOT NULL DEFAULT 1, enableSharedEvents INTEGER NOT NULL DEFAULT 1, registration INTEGER NOT NULL DEFAULT 0, \n" +
                        "userCode TEXT NOT NULL DEFAULT \"\", studentNumber INTEGER NOT NULL DEFAULT -1, studentClassName TEXT DEFAULT NULL, \n" +
                        "studentSchoolYearStart INTEGER NOT NULL, dateSemester1Start TEXT NOT NULL, dateSemester2Start TEXT NOT NULL, \n" +
                        "dateYearEnd TEXT NOT NULL, disabledNotifications TEXT DEFAULT NULL, lastReceiversSync INTEGER NOT NULL DEFAULT 0, \n" +
                        "loginStoreId INTEGER NOT NULL, loginStoreType INTEGER NOT NULL, PRIMARY KEY(profileId));")
                database.execSQL("INSERT INTO profiles (profileId, name, subname, image, studentNameLong, studentNameShort, accountName, \n" +
                        "userCode, studentData, empty, archived, syncEnabled, enableSharedEvents, registration, studentNumber, studentSchoolYearStart, \n" +
                        "dateSemester1Start, dateSemester2Start, dateYearEnd, lastReceiversSync, loginStoreId, loginStoreType \n" +
                        ") SELECT profileId, name, subname, image, studentNameLong, studentNameShort, accountNameLong, \"\" AS userCode, studentData, \n" +
                        "empty, archived, syncEnabled, enableSharedEvents, registration, studentNumber, SUBSTR(dateSemester1Start, 0, 5) AS studentSchoolYearStart, \n" +
                        "dateSemester1Start, dateSemester2Start, dateYearEnd, lastReceiversSync, _profiles_old.loginStoreId, loginStoreType FROM _profiles_old \n" +
                        "JOIN loginStores ON loginStores.loginStoreId = _profiles_old.loginStoreId \n" +
                        "WHERE profileId >= 0;")
                database.execSQL("DROP TABLE _profiles_old;")
                // MIGRACJA userCode - mobidziennik
                database.execSQL("DROP TABLE IF EXISTS _userCodes;")
                database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, serverName TEXT, username TEXT, studentId TEXT);")
                database.execSQL("DELETE FROM _userCodes;")
                database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 1;")
                database.execSQL("UPDATE _userCodes SET serverName = SUBSTR(loginData, instr(loginData, '\"serverName\":\"')+14);")
                database.execSQL("UPDATE _userCodes SET serverName = SUBSTR(serverName, 0, instr(serverName, '\",')+instr(serverName, '\"}')-(instr(serverName, '\"}')*min(instr(serverName, '\",'), 1)));")
                database.execSQL("UPDATE _userCodes SET username = SUBSTR(loginData, instr(loginData, '\"username\":\"')+12);")
                database.execSQL("UPDATE _userCodes SET username = SUBSTR(username, 0, instr(username, '\",')+instr(username, '\"}')-(instr(username, '\"}')*min(instr(username, '\",'), 1)));")
                database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentData, instr(studentData, '\"studentId\":')+12);")
                database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentId, 0, instr(studentId, ',')+instr(studentId, '}')-(instr(studentId, '}')*min(instr(studentId, ','), 1)));")
                database.execSQL("UPDATE _userCodes SET userCode = serverName||\":\"||username||\":\"||studentId;")
                database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);")
                // MIGRACJA userCode - librus
                database.execSQL("DROP TABLE IF EXISTS _userCodes;")
                database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, accountLogin TEXT);")
                database.execSQL("DELETE FROM _userCodes;")
                database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 2;")
                database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(studentData, instr(studentData, '\"schoolName\":\"')+14);")
                database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));")
                database.execSQL("UPDATE _userCodes SET accountLogin = SUBSTR(studentData, instr(studentData, '\"accountLogin\":\"')+16);")
                database.execSQL("UPDATE _userCodes SET accountLogin = SUBSTR(accountLogin, 0, instr(accountLogin, '\",')+instr(accountLogin, '\"}')-(instr(accountLogin, '\"}')*min(instr(accountLogin, '\",'), 1)));")
                database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||accountLogin;")
                database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);")
                // MIGRACJA userCode - iuczniowie
                database.execSQL("DROP TABLE IF EXISTS _userCodes;")
                database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, username TEXT, registerId TEXT);")
                database.execSQL("DELETE FROM _userCodes;")
                database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 3;")
                database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(loginData, instr(loginData, '\"schoolName\":\"')+14);")
                database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));")
                database.execSQL("UPDATE _userCodes SET username = SUBSTR(loginData, instr(loginData, '\"username\":\"')+12);")
                database.execSQL("UPDATE _userCodes SET username = SUBSTR(username, 0, instr(username, '\",')+instr(username, '\"}')-(instr(username, '\"}')*min(instr(username, '\",'), 1)));")
                database.execSQL("UPDATE _userCodes SET registerId = SUBSTR(studentData, instr(studentData, '\"registerId\":')+13);")
                database.execSQL("UPDATE _userCodes SET registerId = SUBSTR(registerId, 0, instr(registerId, ',')+instr(registerId, '}')-(instr(registerId, '}')*min(instr(registerId, ','), 1)));")
                database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||username||\":\"||registerId;")
                database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);")
                // MIGRACJA userCode - vulcan
                database.execSQL("DROP TABLE IF EXISTS _userCodes;")
                database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, studentId TEXT);")
                database.execSQL("DELETE FROM _userCodes;")
                database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 4;")
                database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(studentData, instr(studentData, '\"schoolName\":\"')+14);")
                database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));")
                database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentData, instr(studentData, '\"studentId\":')+12);")
                database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentId, 0, instr(studentId, ',')+instr(studentId, '}')-(instr(studentId, '}')*min(instr(studentId, ','), 1)));")
                database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||studentId;")
                database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);")
                // MIGRACJA userCode - edudziennik
                database.execSQL("DROP TABLE IF EXISTS _userCodes;")
                database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, email TEXT, studentId TEXT);")
                database.execSQL("DELETE FROM _userCodes;")
                database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 5;")
                database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(studentData, instr(studentData, '\"schoolName\":\"')+14);")
                database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));")
                database.execSQL("UPDATE _userCodes SET email = SUBSTR(loginData, instr(loginData, '\"email\":\"')+9);")
                database.execSQL("UPDATE _userCodes SET email = SUBSTR(email, 0, instr(email, '\",')+instr(email, '\"}')-(instr(email, '\"}')*min(instr(email, '\",'), 1)));")
                database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentData, instr(studentData, '\"studentId\":\"')+13);")
                database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentId, 0, instr(studentId, '\",')+instr(studentId, '\"}')-(instr(studentId, '\"}')*min(instr(studentId, '\",'), 1)));")
                database.query("SELECT profileId, studentId FROM _userCodes;").use { cursor ->
                    while (cursor.moveToNext()) {
                        val profileId = cursor.getInt(0)
                        val crc = cursor.getString(1).crc32()
                        database.execSQL("UPDATE _userCodes SET studentId = $crc WHERE profileId = $profileId")
                    }
                }
                database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||email||\":\"||studentId;")
                database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);")
                database.execSQL("DROP TABLE _userCodes;")
            }
        }
        private val MIGRATION_72_73: Migration = object : Migration(72, 73) {
            override fun migrate(database: SupportSQLiteDatabase) { // Mark as seen all lucky number metadata.
                database.execSQL("UPDATE metadata SET seen=1 WHERE thingType=10")
                database.execSQL("DROP TABLE lessons")
                database.execSQL("DROP TABLE lessonChanges")
            }
        }
        private val MIGRATION_73_74: Migration = object : Migration(73, 74) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE librusLessons (" +
                        "profileId INTEGER NOT NULL," +
                        "lessonId INTEGER NOT NULL," +
                        "teacherId INTEGER NOT NULL," +
                        "subjectId INTEGER NOT NULL," +
                        "teamId INTEGER," +
                        "PRIMARY KEY(profileId, lessonId));")
                database.execSQL("CREATE INDEX index_librusLessons_profileId ON librusLessons (profileId);")
            }
        }
        private val MIGRATION_74_75: Migration = object : Migration(74, 75) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE timetable RENAME TO _timetable;")
                database.execSQL("CREATE TABLE timetable (profileId INTEGER NOT NULL,id INTEGER NOT NULL,type INTEGER NOT NULL,date TEXT DEFAULT NULL,lessonNumber INTEGER DEFAULT NULL,startTime TEXT DEFAULT NULL,endTime TEXT DEFAULT NULL,subjectId INTEGER DEFAULT NULL,teacherId INTEGER DEFAULT NULL,teamId INTEGER DEFAULT NULL,classroom TEXT DEFAULT NULL,oldDate TEXT DEFAULT NULL,oldLessonNumber INTEGER DEFAULT NULL,oldStartTime TEXT DEFAULT NULL,oldEndTime TEXT DEFAULT NULL,oldSubjectId INTEGER DEFAULT NULL,oldTeacherId INTEGER DEFAULT NULL,oldTeamId INTEGER DEFAULT NULL,oldClassroom TEXT DEFAULT NULL,PRIMARY KEY(profileId, id));")
                database.execSQL("INSERT INTO timetable (profileId, id, type, date, lessonNumber, startTime, endTime, subjectId, teacherId, teamId, classroom, oldDate, oldLessonNumber, oldStartTime, oldEndTime, oldSubjectId, oldTeacherId, oldTeamId, oldClassroom) SELECT profileId, id, type, date, lessonNumber, startTime, endTime, subjectId, teacherId, teamId, classroom, oldDate, oldLessonNumber, oldStartTime, oldEndTime, oldSubjectId, oldTeacherId, oldTeamId, oldClassroom FROM _timetable;")
                database.execSQL("DROP TABLE _timetable;")
                database.execSQL("CREATE INDEX index_lessons_profileId_type_date ON timetable (profileId, type, date);")
                database.execSQL("CREATE INDEX index_lessons_profileId_type_oldDate ON timetable (profileId, type, oldDate);")
            }
        }

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
                context.applicationContext,
                AppDb::class.java,
                "edziennik.db"
        ).addMigrations(
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20,
                MIGRATION_20_21,
                MIGRATION_21_22,
                MIGRATION_22_23,
                MIGRATION_23_24,
                MIGRATION_24_25,
                MIGRATION_25_26,
                MIGRATION_26_27,
                MIGRATION_27_28,
                MIGRATION_28_29,
                MIGRATION_29_30,
                MIGRATION_30_31,
                MIGRATION_31_32,
                MIGRATION_32_33,
                MIGRATION_33_34,
                MIGRATION_34_35,
                MIGRATION_35_36,
                MIGRATION_36_37,
                MIGRATION_37_38,
                MIGRATION_38_39,
                MIGRATION_39_40,
                MIGRATION_40_41,
                MIGRATION_41_42,
                MIGRATION_42_43,
                MIGRATION_43_44,
                MIGRATION_44_45,
                MIGRATION_45_46,
                MIGRATION_46_47,
                MIGRATION_47_48,
                MIGRATION_48_49,
                MIGRATION_49_50,
                MIGRATION_50_51,
                MIGRATION_51_52,
                MIGRATION_52_53,
                MIGRATION_53_54,
                MIGRATION_54_55,
                MIGRATION_55_56,
                MIGRATION_56_57,
                MIGRATION_57_58,
                MIGRATION_58_59,
                MIGRATION_59_60,
                MIGRATION_60_61,
                MIGRATION_61_62,
                MIGRATION_62_63,
                MIGRATION_63_64,
                MIGRATION_64_65,
                MIGRATION_65_66,
                MIGRATION_66_67,
                MIGRATION_67_68,
                MIGRATION_68_69,
                MIGRATION_69_70,
                MIGRATION_70_71,
                MIGRATION_71_72,
                MIGRATION_72_73,
                MIGRATION_73_74,
                MIGRATION_74_75
        ).allowMainThreadQueries().build()
    }
}