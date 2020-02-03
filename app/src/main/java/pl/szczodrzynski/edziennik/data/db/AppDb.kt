package pl.szczodrzynski.edziennik.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.szczodrzynski.edziennik.config.db.ConfigDao
import pl.szczodrzynski.edziennik.config.db.ConfigEntry
import pl.szczodrzynski.edziennik.data.db.converter.*
import pl.szczodrzynski.edziennik.data.db.dao.*
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.data.db.migration.*

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
], version = 77)
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

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
                context.applicationContext,
                AppDb::class.java,
                "edziennik.db"
        ).addMigrations(
                Migration12(),
                Migration13(),
                Migration14(),
                Migration15(),
                Migration16(),
                Migration17(),
                Migration18(),
                Migration19(),
                Migration20(),
                Migration21(),
                Migration22(),
                Migration23(),
                Migration24(),
                Migration25(),
                Migration26(),
                Migration27(),
                Migration28(),
                Migration29(),
                Migration30(),
                Migration31(),
                Migration32(),
                Migration33(),
                Migration34(),
                Migration35(),
                Migration36(),
                Migration37(),
                Migration38(),
                Migration39(),
                Migration40(),
                Migration41(),
                Migration42(),
                Migration43(),
                Migration44(),
                Migration45(),
                Migration46(),
                Migration47(),
                Migration48(),
                Migration49(),
                Migration50(),
                Migration51(),
                Migration52(),
                Migration53(),
                Migration54(),
                Migration55(),
                Migration56(),
                Migration57(),
                Migration58(),
                Migration59(),
                Migration60(),
                Migration61(),
                Migration62(),
                Migration63(),
                Migration64(),
                Migration65(),
                Migration66(),
                Migration67(),
                Migration68(),
                Migration69(),
                Migration70(),
                Migration71(),
                Migration72(),
                Migration73(),
                Migration74(),
                Migration75(),
                Migration76(),
                Migration77()
        ).allowMainThreadQueries().build()
    }
}
