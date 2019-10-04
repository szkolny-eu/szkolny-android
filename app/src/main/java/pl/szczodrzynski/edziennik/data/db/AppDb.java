package pl.szczodrzynski.edziennik.data.db;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;

import pl.szczodrzynski.edziennik.data.db.modules.announcements.Announcement;
import pl.szczodrzynski.edziennik.data.db.modules.announcements.AnnouncementDao;
import pl.szczodrzynski.edziennik.data.db.modules.api.EndpointTimer;
import pl.szczodrzynski.edziennik.data.db.modules.api.EndpointTimerDao;
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance;
import pl.szczodrzynski.edziennik.data.db.modules.attendance.AttendanceDao;
import pl.szczodrzynski.edziennik.data.db.converters.ConverterDate;
import pl.szczodrzynski.edziennik.data.db.converters.ConverterJsonObject;
import pl.szczodrzynski.edziennik.data.db.converters.ConverterListLong;
import pl.szczodrzynski.edziennik.data.db.converters.ConverterListString;
import pl.szczodrzynski.edziennik.data.db.converters.ConverterTime;
import pl.szczodrzynski.edziennik.data.db.modules.debuglog.DebugLog;
import pl.szczodrzynski.edziennik.data.db.modules.debuglog.DebugLogDao;
import pl.szczodrzynski.edziennik.data.db.modules.events.Event;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventDao;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventTypeDao;
import pl.szczodrzynski.edziennik.data.db.modules.feedback.FeedbackMessage;
import pl.szczodrzynski.edziennik.data.db.modules.feedback.FeedbackMessageDao;
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade;
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory;
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategoryDao;
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeDao;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.Lesson;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChangeDao;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonDao;
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStoreDao;
import pl.szczodrzynski.edziennik.data.db.modules.luckynumber.LuckyNumber;
import pl.szczodrzynski.edziennik.data.db.modules.luckynumber.LuckyNumberDao;
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageDao;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipientDao;
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata;
import pl.szczodrzynski.edziennik.data.db.modules.metadata.MetadataDao;
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice;
import pl.szczodrzynski.edziennik.data.db.modules.notices.NoticeDao;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileDao;
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject;
import pl.szczodrzynski.edziennik.data.db.modules.subjects.SubjectDao;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherAbsence;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherAbsenceDao;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherDao;
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team;
import pl.szczodrzynski.edziennik.data.db.modules.teams.TeamDao;
import pl.szczodrzynski.edziennik.utils.models.Date;

import android.content.Context;

@Database(entities = {
        Grade.class,
        //GradeCategory.class,
        Teacher.class,
        TeacherAbsence.class,
        Subject.class,
        Notice.class,
        Lesson.class,
        LessonChange.class,
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
        Metadata.class}, version = 55)
@TypeConverters({
        ConverterTime.class,
        ConverterDate.class,
        ConverterJsonObject.class,
        ConverterListLong.class,
        ConverterListString.class})
public abstract class AppDb extends RoomDatabase {

    public abstract GradeDao gradeDao();
    //public abstract GradeCategoryDao gradeCategoryDao();
    public abstract TeacherDao teacherDao();
    public abstract TeacherAbsenceDao teacherAbsenceDao();
    public abstract SubjectDao subjectDao();
    public abstract NoticeDao noticeDao();
    public abstract LessonDao lessonDao();
    public abstract LessonChangeDao lessonChangeDao();
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
            database.execSQL("CREATE TABLE IF NOT EXISTS endpointTimers (" +
                    "profileId INTEGER NOT NULL," +
                    "endpointId INTEGER NOT NULL," +
                    "endpointLastSync INTEGER DEFAULT NULL," +
                    "endpointNextSync INTEGER NOT NULL DEFAULT 1," +
                    "endpointViewId INTEGER DEFAULT NULL," +
                    "PRIMARY KEY(profileId, endpointId)" +
                    ")");
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
                                    MIGRATION_54_55
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
