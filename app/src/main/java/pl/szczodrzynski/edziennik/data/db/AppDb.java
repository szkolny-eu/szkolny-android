package pl.szczodrzynski.edziennik.data.db;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import pl.szczodrzynski.edziennik.ExtensionsKt;
import pl.szczodrzynski.edziennik.config.db.ConfigDao;
import pl.szczodrzynski.edziennik.config.db.ConfigEntry;
import pl.szczodrzynski.edziennik.data.db.converter.ConverterDate;
import pl.szczodrzynski.edziennik.data.db.converter.ConverterDateInt;
import pl.szczodrzynski.edziennik.data.db.converter.ConverterJsonObject;
import pl.szczodrzynski.edziennik.data.db.converter.ConverterListLong;
import pl.szczodrzynski.edziennik.data.db.converter.ConverterListString;
import pl.szczodrzynski.edziennik.data.db.converter.ConverterTime;
import pl.szczodrzynski.edziennik.data.db.entity.Announcement;
import pl.szczodrzynski.edziennik.data.db.dao.AnnouncementDao;
import pl.szczodrzynski.edziennik.data.db.entity.EndpointTimer;
import pl.szczodrzynski.edziennik.data.db.dao.EndpointTimerDao;
import pl.szczodrzynski.edziennik.data.db.entity.Attendance;
import pl.szczodrzynski.edziennik.data.db.dao.AttendanceDao;
import pl.szczodrzynski.edziennik.data.db.entity.AttendanceType;
import pl.szczodrzynski.edziennik.data.db.dao.AttendanceTypeDao;
import pl.szczodrzynski.edziennik.data.db.entity.Classroom;
import pl.szczodrzynski.edziennik.data.db.dao.ClassroomDao;
import pl.szczodrzynski.edziennik.data.db.entity.DebugLog;
import pl.szczodrzynski.edziennik.data.db.dao.DebugLogDao;
import pl.szczodrzynski.edziennik.data.db.entity.Event;
import pl.szczodrzynski.edziennik.data.db.dao.EventDao;
import pl.szczodrzynski.edziennik.data.db.entity.EventType;
import pl.szczodrzynski.edziennik.data.db.dao.EventTypeDao;
import pl.szczodrzynski.edziennik.data.db.entity.FeedbackMessage;
import pl.szczodrzynski.edziennik.data.db.dao.FeedbackMessageDao;
import pl.szczodrzynski.edziennik.data.db.entity.Grade;
import pl.szczodrzynski.edziennik.data.db.entity.GradeCategory;
import pl.szczodrzynski.edziennik.data.db.dao.GradeCategoryDao;
import pl.szczodrzynski.edziennik.data.db.dao.GradeDao;
import pl.szczodrzynski.edziennik.data.db.entity.Lesson;
import pl.szczodrzynski.edziennik.data.db.entity.LessonRange;
import pl.szczodrzynski.edziennik.data.db.dao.LessonRangeDao;
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore;
import pl.szczodrzynski.edziennik.data.db.dao.LoginStoreDao;
import pl.szczodrzynski.edziennik.data.db.entity.LuckyNumber;
import pl.szczodrzynski.edziennik.data.db.dao.LuckyNumberDao;
import pl.szczodrzynski.edziennik.data.db.entity.Message;
import pl.szczodrzynski.edziennik.data.db.dao.MessageDao;
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient;
import pl.szczodrzynski.edziennik.data.db.dao.MessageRecipientDao;
import pl.szczodrzynski.edziennik.data.db.entity.Metadata;
import pl.szczodrzynski.edziennik.data.db.dao.MetadataDao;
import pl.szczodrzynski.edziennik.data.db.entity.Notice;
import pl.szczodrzynski.edziennik.data.db.dao.NoticeDao;
import pl.szczodrzynski.edziennik.data.db.entity.NoticeType;
import pl.szczodrzynski.edziennik.data.db.dao.NoticeTypeDao;
import pl.szczodrzynski.edziennik.data.db.entity.Notification;
import pl.szczodrzynski.edziennik.data.db.dao.NotificationDao;
import pl.szczodrzynski.edziennik.data.db.entity.Profile;
import pl.szczodrzynski.edziennik.data.db.dao.ProfileDao;
import pl.szczodrzynski.edziennik.data.db.entity.Subject;
import pl.szczodrzynski.edziennik.data.db.dao.SubjectDao;
import pl.szczodrzynski.edziennik.data.db.entity.Teacher;
import pl.szczodrzynski.edziennik.data.db.entity.TeacherAbsence;
import pl.szczodrzynski.edziennik.data.db.dao.TeacherAbsenceDao;
import pl.szczodrzynski.edziennik.data.db.entity.TeacherAbsenceType;
import pl.szczodrzynski.edziennik.data.db.dao.TeacherAbsenceTypeDao;
import pl.szczodrzynski.edziennik.data.db.dao.TeacherDao;
import pl.szczodrzynski.edziennik.data.db.entity.Team;
import pl.szczodrzynski.edziennik.data.db.dao.TeamDao;
import pl.szczodrzynski.edziennik.data.db.entity.LibrusLesson;
import pl.szczodrzynski.edziennik.data.db.dao.LibrusLessonDao;
import pl.szczodrzynski.edziennik.data.db.dao.TimetableDao;
import pl.szczodrzynski.edziennik.utils.models.Date;

@Database(entities = {
        Grade.class,
        //GradeCategory.class,
        Teacher.class,
        TeacherAbsence.class,
        TeacherAbsenceType.class,
        Subject.class,
        Notice.class,
        Team.class,
        Attendance.class,
        Event.class,
        EventType.class,
        LoginStore.class,
        Profile.class,
        LuckyNumber.class,
        Announcement.class,
        GradeCategory.class,
        FeedbackMessage.class,
        Message.class,
        MessageRecipient.class,
        DebugLog.class,
        EndpointTimer.class,
        LessonRange.class,
        Notification.class,
        Classroom.class,
        NoticeType.class,
        AttendanceType.class,
        Lesson.class,
        ConfigEntry.class,
        LibrusLesson.class,
        Metadata.class}, version = 74)
@TypeConverters({
        ConverterTime.class,
        ConverterDate.class,
        ConverterJsonObject.class,
        ConverterListLong.class,
        ConverterListString.class,
        ConverterDateInt.class})
public abstract class AppDb extends RoomDatabase {

    public abstract GradeDao gradeDao();
    //public abstract GradeCategoryDao gradeCategoryDao();
    public abstract TeacherDao teacherDao();
    public abstract TeacherAbsenceDao teacherAbsenceDao();
    public abstract TeacherAbsenceTypeDao teacherAbsenceTypeDao();
    public abstract SubjectDao subjectDao();
    public abstract NoticeDao noticeDao();
    public abstract TeamDao teamDao();
    public abstract AttendanceDao attendanceDao();
    public abstract EventDao eventDao();
    public abstract EventTypeDao eventTypeDao();
    public abstract LoginStoreDao loginStoreDao();
    public abstract ProfileDao profileDao();
    public abstract LuckyNumberDao luckyNumberDao();
    public abstract AnnouncementDao announcementDao();
    public abstract GradeCategoryDao gradeCategoryDao();
    public abstract FeedbackMessageDao feedbackMessageDao();
    public abstract MessageDao messageDao();
    public abstract MessageRecipientDao messageRecipientDao();
    public abstract DebugLogDao debugLogDao();
    public abstract EndpointTimerDao endpointTimerDao();
    public abstract LessonRangeDao lessonRangeDao();
    public abstract NotificationDao notificationDao();
    public abstract ClassroomDao classroomDao();
    public abstract NoticeTypeDao noticeTypeDao();
    public abstract AttendanceTypeDao attendanceTypeDao();
    public abstract TimetableDao timetableDao();
    public abstract ConfigDao configDao();
    public abstract LibrusLessonDao librusLessonDao();
    public abstract MetadataDao metadataDao();

    private static volatile AppDb INSTANCE;

    private static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("PRAGMA foreign_keys=off;");
            database.execSQL("ALTER TABLE teams RENAME TO _teams_old;");
            database.execSQL("CREATE TABLE teams (profileId INTEGER NOT NULL, teamId INTEGER NOT NULL, teamType INTEGER NOT NULL, teamName TEXT, teamTeacherId INTEGER NOT NULL, PRIMARY KEY(profileId, teamId));");
            database.execSQL("INSERT INTO teams (profileId, teamId, teamType, teamName, teamTeacherId) SELECT profileId, teamId, teamType, teamName, teacherId FROM _teams_old;");
            database.execSQL("DROP TABLE _teams_old;");
            database.execSQL("PRAGMA foreign_keys=on;");
        }
    };
    private static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE lessonChanges ADD lessonChangeWeekDay INTEGER NOT NULL DEFAULT -1;");
        }
    };
    private static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE loginStores (loginStoreId INTEGER NOT NULL, loginStoreType INTEGER NOT NULL, loginStoreData TEXT, PRIMARY KEY(loginStoreId));");
        }
    };
    private static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grades RENAME TO _grades_old;");
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
                    ");");
            database.execSQL("INSERT INTO grades\n" +
                    "   SELECT *\n" +
                    "   FROM _grades_old;");
            database.execSQL("DROP TABLE _grades_old;");
            database.execSQL("CREATE INDEX index_grades_profileId ON grades (profileId);");
        }
    };
    private static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
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
                    "PRIMARY KEY(profileId));");
        }
    };
    private static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE profiles ADD archived INTEGER NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE teams ADD teamCode TEXT;");
        }
    };
    private static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE events ADD eventBlacklisted INTEGER NOT NULL DEFAULT 0;");
        }
    };
    private static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grades ADD gradeClassAverage REAL NOT NULL DEFAULT -1;");
        }
    };
    private static final Migration MIGRATION_19_20 = new Migration(19, 20) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE luckyNumbers (" +
                    "profileId INTEGER NOT NULL, " +
                    "luckyNumberDate TEXT NOT NULL, " +
                    "luckyNumber INTEGER NOT NULL, " +
                    "PRIMARY KEY(profileId, luckyNumberDate));");
        }
    };
    private static final Migration MIGRATION_20_21 = new Migration(20, 21) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grades ADD gradeCategory TEXT");
            database.execSQL("ALTER TABLE grades ADD gradeColor INTEGER NOT NULL DEFAULT -1");
            database.execSQL("DROP TABLE gradeCategories");
        }
    };
    private static final Migration MIGRATION_21_22 = new Migration(21, 22) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE eventTypes (" +
                    "profileId INTEGER NOT NULL, " +
                    "eventType INTEGER NOT NULL, " +
                    "eventTypeName TEXT, " +
                    "eventTypeColor INTEGER NOT NULL, " +
                    "PRIMARY KEY(profileId, eventType));");
        }
    };
    private static final Migration MIGRATION_22_23 = new Migration(22, 23) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grades RENAME TO _grades_old;");
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
                    ");");
            database.execSQL("DROP INDEX index_grades_profileId");
            database.execSQL("CREATE INDEX `index_grades_profileId` ON `grades` (\n" +
                    "\t`profileId`\n" +
                    ");");
            database.execSQL("INSERT INTO grades (profileId, gradeId, gradeDescription, gradeName, gradeValue, gradeWeight, gradeSemester, gradeType, teacherId, subjectId, gradeClassAverage, gradeCategory, gradeColor) SELECT profileId, gradeId, gradeDescription, gradeName, gradeValue, gradeWeight, gradeSemester, gradeType, teacherId, subjectId, gradeClassAverage, gradeCategory, gradeColor FROM _grades_old;");
            database.execSQL("DROP TABLE _grades_old;");

            database.execSQL("ALTER TABLE attendances RENAME TO _attendances_old;");
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
                    ");");
            database.execSQL("DROP INDEX index_attendances_profileId");
            database.execSQL("CREATE INDEX `index_attendances_profileId` ON `attendances` (\n" +
                    "\t`profileId`\n" +
                    ");");
            database.execSQL("INSERT INTO attendances SELECT * FROM _attendances_old;");
            database.execSQL("DROP TABLE _attendances_old;");
        }
    };
    private static final Migration MIGRATION_23_24 = new Migration(23, 24) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE profiles ADD yearAverageMode INTEGER NOT NULL DEFAULT 0");
        }
    };
    private static final Migration MIGRATION_24_25 = new Migration(24, 25) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE announcements (" +
                    "profileId INTEGER NOT NULL, " +
                    "announcementId INTEGER NOT NULL, " +
                    "announcementSubject TEXT, " +
                    "announcementText TEXT, " +
                    "announcementStartDate TEXT, " +
                    "announcementEndDate TEXT, " +
                    "teacherId INTEGER NOT NULL, " +
                    "PRIMARY KEY(profileId, announcementId));");
            //database.execSQL("DROP INDEX index_announcements_profileId");
            database.execSQL("CREATE INDEX index_announcements_profileId ON announcements (" +
                    "profileId" +
                    ");");
        }
    };
    private static final Migration MIGRATION_25_26 = new Migration(25, 26) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grades ADD gradePointGrade INTEGER NOT NULL DEFAULT 0;");
        }
    };
    private static final Migration MIGRATION_26_27 = new Migration(26, 27) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grades ADD gradeValueMax REAL NOT NULL DEFAULT 0;");
        }
    };
    private static final Migration MIGRATION_27_28 = new Migration(27, 28) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE gradeCategories (profileId INTEGER NOT NULL, categoryId INTEGER NOT NULL, weight REAL NOT NULL, color INTEGER NOT NULL, `text` TEXT, columns TEXT, valueFrom REAL NOT NULL, valueTo REAL NOT NULL, PRIMARY KEY(profileId, categoryId));");
            database.execSQL("ALTER TABLE grades ADD gradeComment TEXT;");
        }
    };
    private static final Migration MIGRATION_28_29 = new Migration(28, 29) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE feedbackMessages (messageId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, received INTEGER NOT NULL DEFAULT 0, sentTime INTEGER NOT NULL, `text` TEXT)");
        }
    };
    private static final Migration MIGRATION_29_30 = new Migration(29, 30) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE feedbackMessages ADD fromUser TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE feedbackMessages ADD fromUserName TEXT DEFAULT NULL");
        }
    };
    private static final Migration MIGRATION_30_31 = new Migration(30, 31) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
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
                    "PRIMARY KEY(profileId, messageId));");
            //database.execSQL("DROP INDEX index_announcements_profileId");
            database.execSQL("CREATE INDEX index_messages_profileId ON messages (" +
                    "profileId" +
                    ");");
        }
    };
    private static final Migration MIGRATION_31_32 = new Migration(31, 32) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE messages ADD attachmentSizes TEXT DEFAULT NULL");
        }
    };
    private static final Migration MIGRATION_32_33 = new Migration(32, 33) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE messages");
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
                    "PRIMARY KEY(profileId, messageId));");
            //database.execSQL("DROP INDEX index_announcements_profileId");
            database.execSQL("CREATE INDEX index_messages_profileId ON messages (" +
                    "profileId" +
                    ");");
            database.execSQL("CREATE TABLE messageRecipients (" +
                    "profileId INTEGER NOT NULL, " +
                    "messageRecipientId INTEGER NOT NULL DEFAULT -1, " +
                    "messageRecipientReplyId INTEGER NOT NULL DEFAULT -1, " +
                    "messageRecipientReadDate INTEGER NOT NULL DEFAULT -1, " +
                    "messageId INTEGER NOT NULL, " +
                    "PRIMARY KEY(profileId, messageRecipientId));");
        }
    };
    private static final Migration MIGRATION_33_34 = new Migration(33, 34) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE messageRecipients");
            database.execSQL("CREATE TABLE messageRecipients (" +
                    "profileId INTEGER NOT NULL, " +
                    "messageRecipientId INTEGER NOT NULL DEFAULT -1, " +
                    "messageRecipientReplyId INTEGER NOT NULL DEFAULT -1, " +
                    "messageRecipientReadDate INTEGER NOT NULL DEFAULT -1, " +
                    "messageId INTEGER NOT NULL, " +
                    "PRIMARY KEY(profileId, messageRecipientId, messageId));");
        }
    };
    private static final Migration MIGRATION_34_35 = new Migration(34, 35) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE teachers ADD teacherLoginId TEXT DEFAULT NULL;");
        }
    };
    private static final Migration MIGRATION_35_36 = new Migration(35, 36) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE profiles SET yearAverageMode = 4 WHERE yearAverageMode = 0");
        }
    };
    private static final Migration MIGRATION_36_37 = new Migration(36, 37) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
        }
    };
    private static final Migration MIGRATION_37_38 = new Migration(37, 38) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Date today = Date.getToday();
            int schoolYearStart = today.month < 9 ? today.year-1 : today.year;
            database.execSQL("UPDATE profiles SET dateSemester1Start = '"+schoolYearStart+"-09-01' WHERE dateSemester1Start IS NULL;");
            database.execSQL("UPDATE profiles SET dateSemester2Start = '"+(schoolYearStart+1)+"-02-01' WHERE dateSemester2Start IS NULL;");
            database.execSQL("UPDATE profiles SET dateYearEnd = '"+(schoolYearStart+1)+"-06-30' WHERE dateYearEnd IS NULL;");
        }
    };
    private static final Migration MIGRATION_38_39 = new Migration(38, 39) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE debugLogs (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `text` TEXT);");
        }
    };
    private static final Migration MIGRATION_39_40 = new Migration(39, 40) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE profiles ADD changedEndpoints TEXT DEFAULT NULL;");
        }
    };
    private static final Migration MIGRATION_40_41 = new Migration(40, 41) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE profiles SET empty = 1;");
        }
    };
    private static final Migration MIGRATION_41_42 = new Migration(41, 42) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE profiles SET empty = 1;");
        }
    };
    private static final Migration MIGRATION_42_43 = new Migration(42, 43) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE profiles ADD lastFullSync INTEGER NOT NULL DEFAULT 0;");
        }
    };
    private static final Migration MIGRATION_43_44 = new Migration(43, 44) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE profiles SET empty = 1;");
            database.execSQL("UPDATE profiles SET currentSemester = 1;");
        }
    };
    private static final Migration MIGRATION_44_45 = new Migration(44, 45) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE profiles SET empty = 1;");
        }
    };
    private static final Migration MIGRATION_45_46 = new Migration(45, 46) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE profiles SET lastFullSync = 0");
        }
    };
    private static final Migration MIGRATION_46_47 = new Migration(46, 47) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE profiles SET lastFullSync = 0");
        }
    };
    private static final Migration MIGRATION_47_48 = new Migration(47, 48) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE notices ADD points REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE notices ADD category TEXT DEFAULT NULL");
        }
    };
    private static final Migration MIGRATION_48_49 = new Migration(48, 49) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grades ADD gradeParentId INTEGER NOT NULL DEFAULT -1");
        }
    };
    private static final Migration MIGRATION_49_50 = new Migration(49, 50) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE grades ADD gradeIsImprovement INTEGER NOT NULL DEFAULT 0");
            database.execSQL("DELETE FROM attendances WHERE attendances.profileId IN (SELECT profiles.profileId FROM profiles JOIN loginStores USING(loginStoreId) WHERE loginStores.loginStoreType = 1)");
            database.execSQL("UPDATE profiles SET empty = 1 WHERE profileId IN (SELECT profiles.profileId FROM profiles JOIN loginStores USING(loginStoreId) WHERE loginStores.loginStoreType = 4)");
        }
    };
    private static final Migration MIGRATION_50_51 = new Migration(50, 51) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE profiles ADD lastReceiversSync INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE teachers ADD teacherType INTEGER NOT NULL DEFAULT 0");
        }
    };
    private static final Migration MIGRATION_51_52 = new Migration(51, 52) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE teachers ADD teacherTypeDescription TEXT DEFAULT NULL");
        }
    };
    private static final Migration MIGRATION_52_53 = new Migration(52, 53) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS teacherAbsence (" +
                    "profileId INTEGER NOT NULL," +
                    "teacherAbsenceId INTEGER NOT NULL," +
                    "teacherId INTEGER NOT NULL," +
                    "teacherAbsenceType INTEGER NOT NULL," +
                    "teacherAbsenceDateFrom TEXT NOT NULL," +
                    "teacherAbsenceDateTo TEXT NOT NULL," +
                    "PRIMARY KEY(profileId, teacherAbsenceId)" +
                    ")");
        }
    };
    private static final Migration MIGRATION_53_54 = new Migration(53, 54) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE teacherAbsence ADD teacherAbsenceTimeFrom TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE teacherAbsence ADD teacherAbsenceTimeTo TEXT DEFAULT NULL");
        }
    };
    private static final Migration MIGRATION_54_55 = new Migration(54, 55) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 2019-10-21 for merge compatibility between 3.1.1 and api-v2
            // moved to Migration 55->56
        }
    };
    private static final Migration MIGRATION_55_56 = new Migration(55, 56) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS endpointTimers (" +
                    "profileId INTEGER NOT NULL," +
                    "endpointId INTEGER NOT NULL," +
                    "endpointLastSync INTEGER DEFAULT NULL," +
                    "endpointNextSync INTEGER NOT NULL DEFAULT 1," +
                    "endpointViewId INTEGER DEFAULT NULL," +
                    "PRIMARY KEY(profileId, endpointId)" +
                    ")");
            database.execSQL("CREATE TABLE IF NOT EXISTS lessonRanges (" +
                    "profileId INTEGER NOT NULL," +
                    "lessonRangeNumber INTEGER NOT NULL," +
                    "lessonRangeStart TEXT NOT NULL," +
                    "lessonRangeEnd TEXT NOT NULL," +
                    "PRIMARY KEY(profileId, lessonRangeNumber)" +
                    ")");
        }
    };
    private static final Migration MIGRATION_56_57 = new Migration(56, 57) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE gradeCategories ADD type INTEGER NOT NULL DEFAULT 0");
        }
    };
    private static final Migration MIGRATION_57_58 = new Migration(57, 58) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE metadata RENAME TO _metadata_old;");
            database.execSQL("DROP INDEX index_metadata_profileId_thingType_thingId;");
            database.execSQL("UPDATE _metadata_old SET thingType = "+Metadata.TYPE_HOMEWORK+" WHERE thingType = "+Metadata.TYPE_EVENT+" AND thingId IN (SELECT eventId FROM events WHERE eventType = -1);");
            database.execSQL("CREATE TABLE metadata (\n"+
                    "profileId INTEGER NOT NULL,\n"+
                    "metadataId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n"+
                    "thingType INTEGER NOT NULL,\n"+
                    "thingId INTEGER NOT NULL,\n"+
                    "seen INTEGER NOT NULL,\n"+
                    "notified INTEGER NOT NULL,\n"+
                    "addedDate INTEGER NOT NULL);");
            database.execSQL("INSERT INTO metadata SELECT * FROM (SELECT * FROM _metadata_old ORDER BY addedDate DESC) GROUP BY thingId;");
            database.execSQL("DROP TABLE _metadata_old;");
            database.execSQL("CREATE UNIQUE INDEX index_metadata_profileId_thingType_thingId ON metadata (profileId, thingType, thingId);");
        }
    };
    private static final Migration MIGRATION_58_59 = new Migration(58, 59) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("UPDATE metadata SET addedDate = addedDate*1000 WHERE addedDate < 10000000000;");
            database.execSQL("INSERT INTO metadata (profileId, thingType, thingId, seen, notified, addedDate)\n" +
                    "SELECT profileId,\n" +
                    "10 AS thingType,\n" +
                    "luckyNumberDate*10000+substr(luckyNumberDate, 6)*100+substr(luckyNumberDate, 9) AS thingId,\n" +
                    "1 AS seen,\n" +
                    "1 AS notified,\n" +
                    "CAST(strftime('%s', luckyNumberDate) AS INT)*1000 AS addedDate\n" +
                    "FROM luckyNumbers;");
            database.execSQL("ALTER TABLE luckyNumbers RENAME TO _old_luckyNumbers;");
            database.execSQL("CREATE TABLE luckyNumbers (\n" +
                    "profileId INTEGER NOT NULL,\n" +
                    "luckyNumberDate INTEGER NOT NULL, \n" +
                    "luckyNumber INTEGER NOT NULL, \n" +
                    "PRIMARY KEY(profileId, luckyNumberDate))");
            database.execSQL("INSERT INTO luckyNumbers (profileId, luckyNumberDate, luckyNumber)\n" +
                    "SELECT profileId,\n" +
                    "luckyNumberDate*10000+substr(luckyNumberDate, 6)*100+substr(luckyNumberDate, 9) AS luckyNumberDate,\n" +
                    "luckyNumber\n" +
                    "FROM _old_luckyNumbers;");
            database.execSQL("DROP TABLE _old_luckyNumbers;");
        }
    };
    private static final Migration MIGRATION_59_60 = new Migration(59, 60) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
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
                    ");");

            database.execSQL("ALTER TABLE profiles ADD COLUMN disabledNotifications TEXT DEFAULT NULL");
        }
    };
    private static final Migration MIGRATION_60_61 = new Migration(60, 61) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS teacherAbsenceTypes (" +
                    "profileId INTEGER NOT NULL," +
                    "teacherAbsenceTypeId INTEGER NOT NULL," +
                    "teacherAbsenceTypeName TEXT NOT NULL," +
                    "PRIMARY KEY(profileId, teacherAbsenceTypeId))");
            database.execSQL("ALTER TABLE teacherAbsence ADD COLUMN teacherAbsenceName TEXT DEFAULT NULL");
        }
    };
    private static final Migration MIGRATION_61_62 = new Migration(61, 62) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS classrooms (\n" +
                    "    profileId INTEGER NOT NULL,\n" +
                    "    id INTEGER NOT NULL,\n" +
                    "    name TEXT NOT NULL,\n" +
                    "    PRIMARY KEY(profileId, id)\n" +
                    ")");
            database.execSQL("CREATE TABLE IF NOT EXISTS noticeTypes (\n" +
                    "    profileId INTEGER NOT NULL,\n" +
                    "    id INTEGER NOT NULL,\n" +
                    "    name TEXT NOT NULL,\n" +
                    "    PRIMARY KEY(profileId, id)\n" +
                    ")");
            database.execSQL("CREATE TABLE IF NOT EXISTS attendanceTypes (\n" +
                    "    profileId INTEGER NOT NULL,\n" +
                    "    id INTEGER NOT NULL,\n" +
                    "    name TEXT NOT NULL,\n" +
                    "    type INTEGER NOT NULL,\n" +
                    "    color INTEGER NOT NULL,\n" +
                    "    PRIMARY KEY(profileId, id)\n" +
                    ")");
        }
    };
    private static final Migration MIGRATION_62_63 = new Migration(62, 63) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE profiles ADD COLUMN accountNameLong TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE profiles ADD COLUMN studentClassName TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE profiles ADD COLUMN studentSchoolYear TEXT DEFAULT NULL");
        }
    };
    private static final Migration MIGRATION_63_64 = new Migration(63, 64) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
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
                    "PRIMARY KEY(id));");
            database.execSQL("CREATE INDEX index_lessons_profileId_type_date ON timetable (profileId, type, date);");
            database.execSQL("CREATE INDEX index_lessons_profileId_type_oldDate ON timetable (profileId, type, oldDate);");
        }
    };
    private static final Migration MIGRATION_64_65 = new Migration(64, 65) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE config (" +
                    "profileId INTEGER NOT NULL DEFAULT -1," +
                    "`key` TEXT NOT NULL," +
                    "value TEXT NOT NULL," +
                    "PRIMARY KEY(profileId, `key`));");
        }
    };
    private static final Migration MIGRATION_65_66 = new Migration(65, 66) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE config;");
            database.execSQL("CREATE TABLE config (" +
                    "profileId INTEGER NOT NULL DEFAULT -1," +
                    "`key` TEXT NOT NULL," +
                    "value TEXT," +
                    "PRIMARY KEY(profileId, `key`));");
        }
    };
    private static final Migration MIGRATION_66_67 = new Migration(66, 67) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM grades WHERE (gradeId=-1 OR gradeId=-2) AND gradeType=20");
            database.execSQL("DELETE FROM metadata WHERE (thingId=-1 OR thingId=-2) AND thingType=1");

            database.execSQL("ALTER TABLE gradeCategories RENAME TO _gradeCategories");
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
                    "PRIMARY KEY(profileId, categoryId, type))");

            database.execSQL("INSERT INTO gradeCategories (profileId, categoryId, weight, color," +
                    "`text`, columns, valueFrom, valueTo, type) " +
                    "SELECT profileId, categoryId, weight, color, `text`, columns, valueFrom," +
                    "valueTo, type FROM _gradeCategories");

            database.execSQL("DROP TABLE _gradeCategories");
        }
    };
    private static final Migration MIGRATION_67_68 = new Migration(67, 68) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            /* Migration from crc16 to crc32 id */
            database.execSQL("DELETE FROM announcements");
            database.execSQL("DELETE FROM metadata WHERE thingType=7");
        }
    };
    private static final Migration MIGRATION_68_69 = new Migration(68, 69) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE loginStores ADD COLUMN loginStoreMode INTEGER NOT NULL DEFAULT 0");
        }
    };
    private static final Migration MIGRATION_69_70 = new Migration(69, 70) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE announcements ADD COLUMN announcementIdString TEXT DEFAULT NULL");
            database.execSQL("DELETE FROM announcements");
        }
    };
    private static final Migration MIGRATION_70_71 = new Migration(70, 71) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM messages WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);");
            database.execSQL("DELETE FROM messageRecipients WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);");
            database.execSQL("DELETE FROM teachers WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);");
            database.execSQL("DELETE FROM endpointTimers WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);");
            database.execSQL("DELETE FROM metadata WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0) AND thingType = 8;");
            database.execSQL("UPDATE profiles SET empty = 1 WHERE archived = 0;");
            database.execSQL("UPDATE profiles SET lastReceiversSync = 0 WHERE archived = 0;");
            database.execSQL("INSERT INTO config (profileId, `key`, value) VALUES (-1, \"runSync\", \"true\");");
        }
    };
    private static final Migration MIGRATION_71_72 = new Migration(71, 72) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE loginStores RENAME to _loginStores;");
            database.execSQL("CREATE TABLE loginStores(" +
                    "loginStoreId INTEGER NOT NULL," +
                    "loginStoreType INTEGER NOT NULL," +
                    "loginStoreMode INTEGER NOT NULL," +
                    "loginStoreData TEXT NOT NULL," +
                    "PRIMARY KEY(loginStoreId));");
            database.execSQL("INSERT INTO loginStores " +
                    "(loginStoreId, loginStoreType, loginStoreMode, loginStoreData) " +
                    "SELECT loginStoreId, loginStoreType, loginStoreMode, loginStoreData " +
                    "FROM _loginStores;");
            database.execSQL("DROP TABLE _loginStores;");

            database.execSQL("ALTER TABLE profiles RENAME TO _profiles_old;");
            database.execSQL("CREATE TABLE profiles (\n" +
                    "profileId INTEGER NOT NULL, name TEXT NOT NULL, subname TEXT, image TEXT DEFAULT NULL, \n" +
                    "studentNameLong TEXT NOT NULL, studentNameShort TEXT NOT NULL, accountName TEXT, \n" +
                    "studentData TEXT NOT NULL, empty INTEGER NOT NULL DEFAULT 1, archived INTEGER NOT NULL DEFAULT 0, \n" +
                    "syncEnabled INTEGER NOT NULL DEFAULT 1, enableSharedEvents INTEGER NOT NULL DEFAULT 1, registration INTEGER NOT NULL DEFAULT 0, \n" +
                    "userCode TEXT NOT NULL DEFAULT \"\", studentNumber INTEGER NOT NULL DEFAULT -1, studentClassName TEXT DEFAULT NULL, \n" +
                    "studentSchoolYearStart INTEGER NOT NULL, dateSemester1Start TEXT NOT NULL, dateSemester2Start TEXT NOT NULL, \n" +
                    "dateYearEnd TEXT NOT NULL, disabledNotifications TEXT DEFAULT NULL, lastReceiversSync INTEGER NOT NULL DEFAULT 0, \n" +
                    "loginStoreId INTEGER NOT NULL, loginStoreType INTEGER NOT NULL, PRIMARY KEY(profileId));");
            database.execSQL("INSERT INTO profiles (profileId, name, subname, image, studentNameLong, studentNameShort, accountName, \n" +
                    "userCode, studentData, empty, archived, syncEnabled, enableSharedEvents, registration, studentNumber, studentSchoolYearStart, \n" +
                    "dateSemester1Start, dateSemester2Start, dateYearEnd, lastReceiversSync, loginStoreId, loginStoreType \n" +
                    ") SELECT profileId, name, subname, image, studentNameLong, studentNameShort, accountNameLong, \"\" AS userCode, studentData, \n" +
                    "empty, archived, syncEnabled, enableSharedEvents, registration, studentNumber, SUBSTR(dateSemester1Start, 0, 5) AS studentSchoolYearStart, \n" +
                    "dateSemester1Start, dateSemester2Start, dateYearEnd, lastReceiversSync, _profiles_old.loginStoreId, loginStoreType FROM _profiles_old \n" +
                    "JOIN loginStores ON loginStores.loginStoreId = _profiles_old.loginStoreId \n" +
                    "WHERE profileId >= 0;");
            database.execSQL("DROP TABLE _profiles_old;");

            // MIGRACJA userCode - mobidziennik
            database.execSQL("DROP TABLE IF EXISTS _userCodes;");
            database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, serverName TEXT, username TEXT, studentId TEXT);");
            database.execSQL("DELETE FROM _userCodes;");
            database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 1;");
            database.execSQL("UPDATE _userCodes SET serverName = SUBSTR(loginData, instr(loginData, '\"serverName\":\"')+14);");
            database.execSQL("UPDATE _userCodes SET serverName = SUBSTR(serverName, 0, instr(serverName, '\",')+instr(serverName, '\"}')-(instr(serverName, '\"}')*min(instr(serverName, '\",'), 1)));");
            database.execSQL("UPDATE _userCodes SET username = SUBSTR(loginData, instr(loginData, '\"username\":\"')+12);");
            database.execSQL("UPDATE _userCodes SET username = SUBSTR(username, 0, instr(username, '\",')+instr(username, '\"}')-(instr(username, '\"}')*min(instr(username, '\",'), 1)));");
            database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentData, instr(studentData, '\"studentId\":')+12);");
            database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentId, 0, instr(studentId, ',')+instr(studentId, '}')-(instr(studentId, '}')*min(instr(studentId, ','), 1)));");
            database.execSQL("UPDATE _userCodes SET userCode = serverName||\":\"||username||\":\"||studentId;");
            database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);");
            // MIGRACJA userCode - librus
            database.execSQL("DROP TABLE IF EXISTS _userCodes;");
            database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, accountLogin TEXT);");
            database.execSQL("DELETE FROM _userCodes;");
            database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 2;");
            database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(studentData, instr(studentData, '\"schoolName\":\"')+14);");
            database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));");
            database.execSQL("UPDATE _userCodes SET accountLogin = SUBSTR(studentData, instr(studentData, '\"accountLogin\":\"')+16);");
            database.execSQL("UPDATE _userCodes SET accountLogin = SUBSTR(accountLogin, 0, instr(accountLogin, '\",')+instr(accountLogin, '\"}')-(instr(accountLogin, '\"}')*min(instr(accountLogin, '\",'), 1)));");
            database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||accountLogin;");
            database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);");
            // MIGRACJA userCode - iuczniowie
            database.execSQL("DROP TABLE IF EXISTS _userCodes;");
            database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, username TEXT, registerId TEXT);");
            database.execSQL("DELETE FROM _userCodes;");
            database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 3;");
            database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(loginData, instr(loginData, '\"schoolName\":\"')+14);");
            database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));");
            database.execSQL("UPDATE _userCodes SET username = SUBSTR(loginData, instr(loginData, '\"username\":\"')+12);");
            database.execSQL("UPDATE _userCodes SET username = SUBSTR(username, 0, instr(username, '\",')+instr(username, '\"}')-(instr(username, '\"}')*min(instr(username, '\",'), 1)));");
            database.execSQL("UPDATE _userCodes SET registerId = SUBSTR(studentData, instr(studentData, '\"registerId\":')+13);");
            database.execSQL("UPDATE _userCodes SET registerId = SUBSTR(registerId, 0, instr(registerId, ',')+instr(registerId, '}')-(instr(registerId, '}')*min(instr(registerId, ','), 1)));");
            database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||username||\":\"||registerId;");
            database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);");
            // MIGRACJA userCode - vulcan
            database.execSQL("DROP TABLE IF EXISTS _userCodes;");
            database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, studentId TEXT);");
            database.execSQL("DELETE FROM _userCodes;");
            database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 4;");
            database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(studentData, instr(studentData, '\"schoolName\":\"')+14);");
            database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));");
            database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentData, instr(studentData, '\"studentId\":')+12);");
            database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentId, 0, instr(studentId, ',')+instr(studentId, '}')-(instr(studentId, '}')*min(instr(studentId, ','), 1)));");
            database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||studentId;");
            database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);");
            // MIGRACJA userCode - edudziennik
            database.execSQL("DROP TABLE IF EXISTS _userCodes;");
            database.execSQL("CREATE TABLE _userCodes (profileId INTEGER, loginData TEXT, studentData TEXT, userCode TEXT, schoolName TEXT, email TEXT, studentId TEXT);");
            database.execSQL("DELETE FROM _userCodes;");
            database.execSQL("INSERT INTO _userCodes SELECT profileId, loginStores.loginStoreData, studentData, \"\", \"\", \"\", \"\" FROM profiles JOIN loginStores ON loginStores.loginStoreId = profiles.loginStoreId WHERE profiles.loginStoreType = 5;");
            database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(studentData, instr(studentData, '\"schoolName\":\"')+14);");
            database.execSQL("UPDATE _userCodes SET schoolName = SUBSTR(schoolName, 0, instr(schoolName, '\",')+instr(schoolName, '\"}')-(instr(schoolName, '\"}')*min(instr(schoolName, '\",'), 1)));");
            database.execSQL("UPDATE _userCodes SET email = SUBSTR(loginData, instr(loginData, '\"email\":\"')+9);");
            database.execSQL("UPDATE _userCodes SET email = SUBSTR(email, 0, instr(email, '\",')+instr(email, '\"}')-(instr(email, '\"}')*min(instr(email, '\",'), 1)));");
            database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentData, instr(studentData, '\"studentId\":\"')+13);");
            database.execSQL("UPDATE _userCodes SET studentId = SUBSTR(studentId, 0, instr(studentId, '\",')+instr(studentId, '\"}')-(instr(studentId, '\"}')*min(instr(studentId, '\",'), 1)));");
            // CRC32 Student IDs
            try (Cursor cursor = database.query("SELECT profileId, studentId FROM _userCodes;")) {
                while (cursor.moveToNext()) {
                    int profileId = cursor.getInt(0);
                    long crc = ExtensionsKt.crc32(cursor.getString(1));
                    database.execSQL("UPDATE _userCodes SET studentId = "+crc+" WHERE profileId = "+profileId);
                }
            }
            database.execSQL("UPDATE _userCodes SET userCode = schoolName||\":\"||email||\":\"||studentId;");
            database.execSQL("UPDATE profiles SET userCode = (SELECT userCode FROM _userCodes WHERE profileId = profiles.profileId) WHERE profileId IN (SELECT profileId FROM _userCodes);");
            database.execSQL("DROP TABLE _userCodes;");
        }
    };
    public static final Migration MIGRATION_72_73 = new Migration(72, 73) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Mark as seen all lucky number metadata.
            database.execSQL("UPDATE metadata SET seen=1 WHERE thingType=10");

            database.execSQL("DROP TABLE lessons");
            database.execSQL("DROP TABLE lessonChanges");
        }
    };
    public static final Migration MIGRATION_73_74 = new Migration(73, 74) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE librusLessons (" +
                    "profileId INTEGER NOT NULL," +
                    "lessonId INTEGER NOT NULL," +
                    "teacherId INTEGER NOT NULL," +
                    "subjectId INTEGER NOT NULL," +
                    "teamId INTEGER," +
                    "PRIMARY KEY(profileId, lessonId));");
            database.execSQL("CREATE INDEX index_librusLessons_profileId ON librusLessons (profileId);");
        }
    };


    public static AppDb getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDb.class, "edziennik.db")
                            .addMigrations(
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
                                    MIGRATION_73_74
                            )
                            .allowMainThreadQueries()
                            //.fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
