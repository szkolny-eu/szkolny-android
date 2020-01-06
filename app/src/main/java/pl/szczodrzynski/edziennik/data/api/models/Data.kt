package pl.szczodrzynski.edziennik.data.api.models

import android.util.LongSparseArray
import android.util.SparseArray
import androidx.core.util.size
import androidx.room.OnConflictStrategy
import com.google.gson.JsonObject
import im.wangchao.mhttp.Response
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.interfaces.EndpointCallback
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.modules.announcements.Announcement
import pl.szczodrzynski.edziennik.data.db.modules.api.EndpointTimer
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance
import pl.szczodrzynski.edziennik.data.db.modules.attendance.AttendanceType
import pl.szczodrzynski.edziennik.data.db.modules.classrooms.Classroom
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory
import pl.szczodrzynski.edziennik.data.db.modules.lessonrange.LessonRange
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.luckynumber.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice
import pl.szczodrzynski.edziennik.data.db.modules.notices.NoticeType
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherAbsence
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherAbsenceType
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.toSparseArray
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.values
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

abstract class Data(val app: App, val profile: Profile?, val loginStore: LoginStore) {
    companion object {
        private const val TAG = "Data"
    }

    var fakeLogin = false

    var cancelled = false

    val profileId
        get() = profile?.id ?: -1

    var arguments: JsonObject? = null

    /**
     * A callback passed to all [Feature]s and [LoginMethod]s
     */
    lateinit var callback: EndpointCallback

    /**
     * A list of [LoginMethod]s *already fulfilled* during this sync.
     *
     * A [LoginMethod] may add elements to this list only after a successful login
     * with that method.
     */
    val loginMethods = mutableListOf<Int>()

    /**
     * A method which may be overridden in child Data* classes.
     *
     * Calling it should populate [loginMethods] with all
     * already available login methods (e.g. a non-expired OAuth token).
     */
    open fun satisfyLoginMethods() {}

    /**
     * A list of Login method IDs that are still pending
     * to run.
     */
    var targetLoginMethodIds = mutableListOf<Int>()
    /**
     * A list of endpoint IDs that are still pending
     * to run.
     */
    var targetEndpointIds = mutableListOf<Int>()
    /**
     * A count of all network requests to do.
     */
    var progressCount: Int = 0
    /**
     * A number by which the progress will be incremented, every time
     * a login method/endpoint finishes its job.
     */
    var progressStep: Float = 0f

    /**
     * A map of endpoint IDs to JSON objects, specifying their arguments bundle.
     */
    var endpointArgs = mutableMapOf<Int, JsonObject>()

    var endpointTimers = mutableListOf<EndpointTimer>()

    val notifications = mutableListOf<Notification>()

    val teacherList = LongSparseArray<Teacher>()
    val subjectList = LongSparseArray<Subject>()
    val teamList = LongSparseArray<Team>()
    val lessonRanges = SparseArray<LessonRange>()
    val gradeCategories = LongSparseArray<GradeCategory>()

    var teacherOnConflictStrategy = OnConflictStrategy.IGNORE

    val classrooms = LongSparseArray<Classroom>()
    val attendanceTypes = LongSparseArray<AttendanceType>()
    val noticeTypes = LongSparseArray<NoticeType>()
    val eventTypes = LongSparseArray<EventType>()
    val teacherAbsenceTypes = LongSparseArray<TeacherAbsenceType>()

    private var mTeamClass: Team? = null
    var teamClass: Team?
        get() {
            if (mTeamClass == null)
                mTeamClass = teamList.singleOrNull { it.type == Team.TYPE_CLASS }
            return mTeamClass
        }
        set(value) {
            mTeamClass = value
        }

    var toRemove = mutableListOf<DataRemoveModel>()

    val lessonList = mutableListOf<Lesson>()

    val gradeList = mutableListOf<Grade>()

    val eventList = mutableListOf<Event>()

    val noticeList = mutableListOf<Notice>()

    val attendanceList = mutableListOf<Attendance>()

    val announcementList = mutableListOf<Announcement>()
    val announcementIgnoreList = mutableListOf<Announcement>()

    val luckyNumberList = mutableListOf<LuckyNumber>()

    val teacherAbsenceList = mutableListOf<TeacherAbsence>()

    val messageList = mutableListOf<Message>()
    val messageIgnoreList = mutableListOf<Message>()
    val messageRecipientList = mutableListOf<MessageRecipient>()
    val messageRecipientIgnoreList = mutableListOf<MessageRecipient>()

    val metadataList = mutableListOf<Metadata>()
    val setSeenMetadataList = mutableListOf<Metadata>()

    val db: AppDb by lazy { app.db }

    init {
        if (App.devMode) {
            fakeLogin = loginStore.hasLoginData("fakeLogin")
        }
        clear()
        if (profile != null) {
            endpointTimers = db.endpointTimerDao().getAllNow(profile.id).toMutableList()
            db.teacherDao().getAllNow(profileId).toSparseArray(teacherList) { it.id }
            db.subjectDao().getAllNow(profileId).toSparseArray(subjectList) { it.id }
            db.teamDao().getAllNow(profileId).toSparseArray(teamList) { it.id }
            db.lessonRangeDao().getAllNow(profileId).toSparseArray(lessonRanges) { it.lessonNumber }
            db.gradeCategoryDao().getAllNow(profileId).toSparseArray(gradeCategories) { it.categoryId }
        }
    }

    fun clear() {
        loginMethods.clear()

        toRemove.clear()
        endpointTimers.clear()
        teacherList.clear()
        subjectList.clear()
        teamList.clear()
        lessonRanges.clear()
        gradeCategories.clear()

        classrooms.clear()
        attendanceTypes.clear()
        noticeTypes.clear()
        eventTypes.clear()
        teacherAbsenceTypes.clear()

        lessonList.clear()
        gradeList.clear()
        noticeList.clear()
        attendanceList.clear()
        announcementList.clear()
        announcementIgnoreList.clear()
        luckyNumberList.clear()
        teacherAbsenceList.clear()
        messageList.clear()
        messageIgnoreList.clear()
        messageRecipientList.clear()
        messageRecipientIgnoreList.clear()
        metadataList.clear()
        setSeenMetadataList.clear()
    }

    open fun saveData() {
        if (profile == null)
            return // return on first login

        profile.empty = false
        profile.userCode = generateUserCode()

        db.profileDao().add(profile)
        db.loginStoreDao().add(loginStore)

        if (profile.id == app.profile?.id) {
            app.profile.apply {
                name = profile.name
                subname = profile.subname
                syncEnabled = profile.syncEnabled
                empty = profile.empty
                archived = profile.archived
                studentNameLong = profile.studentNameLong
                studentNameShort = profile.studentNameShort
                studentNumber = profile.studentNumber
                accountName = profile.accountName
                dateSemester1Start = profile.dateSemester1Start
                dateSemester2Start = profile.dateSemester2Start
                dateYearEnd = profile.dateYearEnd
                lastReceiversSync = profile.lastReceiversSync
            }
        }

        // always present and not empty, during every sync
        db.endpointTimerDao().addAll(endpointTimers)
        if (teacherOnConflictStrategy == OnConflictStrategy.IGNORE)
            db.teacherDao().addAllIgnore(teacherList.values())
        else if (teacherOnConflictStrategy == OnConflictStrategy.REPLACE)
            db.teacherDao().addAll(teacherList.values())
        db.subjectDao().addAll(subjectList.values())
        db.teamDao().clear(profileId)
        db.teamDao().addAll(teamList.values())
        db.lessonRangeDao().clear(profileId)
        db.lessonRangeDao().addAll(lessonRanges.values())
        db.gradeCategoryDao().clear(profileId)
        db.gradeCategoryDao().addAll(gradeCategories.values())

        // may be empty - extracted from DB on demand, by an endpoint
        if (classrooms.size > 0)
            db.classroomDao().addAll(classrooms.values())
        if (attendanceTypes.size > 0)
            db.attendanceTypeDao().addAll(attendanceTypes.values())
        if (noticeTypes.size > 0)
            db.noticeTypeDao().addAll(noticeTypes.values())
        if (eventTypes.size > 0)
            db.eventTypeDao().addAll(eventTypes.values())
        if (teacherAbsenceTypes.size > 0)
            db.teacherAbsenceTypeDao().addAll(teacherAbsenceTypes.values())

        // clear DB with DataRemoveModels added by endpoints
        for (model in toRemove) {
            when (model) {
                is DataRemoveModel.Timetable -> model.commit(profileId, db.timetableDao())
                is DataRemoveModel.Grades -> model.commit(profileId, db.gradeDao())
                is DataRemoveModel.Events -> model.commit(profileId, db.eventDao())
            }
        }

        if (metadataList.isNotEmpty())
            db.metadataDao().addAllIgnore(metadataList)
        if (setSeenMetadataList.isNotEmpty())
            db.metadataDao().setSeen(setSeenMetadataList)

        if (lessonList.isNotEmpty()) {
            db.timetableDao() += lessonList
        }
        if (gradeList.isNotEmpty()) {
            db.gradeDao().addAll(gradeList)
        }
        if (eventList.isNotEmpty()) {
            db.eventDao().addAll(eventList)
        }
        if (noticeList.isNotEmpty()) {
            db.noticeDao().clear(profile.id)
            db.noticeDao().addAll(noticeList)
        }
        if (attendanceList.isNotEmpty())
            db.attendanceDao().addAll(attendanceList)
        if (announcementList.isNotEmpty())
            db.announcementDao().addAll(announcementList)
        if (announcementIgnoreList.isNotEmpty())
            db.announcementDao().addAllIgnore(announcementIgnoreList)
        if (luckyNumberList.isNotEmpty())
            db.luckyNumberDao().addAll(luckyNumberList)
        if (teacherAbsenceList.isNotEmpty())
            db.teacherAbsenceDao().addAll(teacherAbsenceList)

        if (messageList.isNotEmpty())
            db.messageDao().addAll(messageList)
        if (messageIgnoreList.isNotEmpty())
            db.messageDao().addAllIgnore(messageIgnoreList)
        if (messageRecipientList.isNotEmpty())
            db.messageRecipientDao().addAll(messageRecipientList)
        if (messageRecipientIgnoreList.isNotEmpty())
            db.messageRecipientDao().addAllIgnore(messageRecipientIgnoreList)
    }

    fun notify(onSuccess: () -> Unit) {
        if (profile == null) {
            onSuccess()
            return
        }
        try {
            DataNotifications(this)
            db.notificationDao().addAll(notifications)
            onSuccess()
        } catch (e: Exception) {
            error(ApiError(TAG, EXCEPTION_NOTIFY)
                    .withThrowable(e))
        }
    }

    fun setSyncNext(endpointId: Int, syncIn: Long? = null, viewId: Int? = null, syncAt: Long? = null) {
        EndpointTimer(profile?.id ?: -1, endpointId).apply {
            syncedNow()

            if (syncIn != null) {
                if (syncIn < 10)
                    nextSync = syncIn
                else
                    syncIn(syncIn)
            }
            if (syncAt != null) {
                nextSync = syncAt
            }
            if (viewId != null)
                syncWhenView(viewId)

            endpointTimers.add(this)
        }
    }

    abstract fun generateUserCode(): String

    fun cancel() {
        d("Data", "Cancelled")
        cancelled = true
        saveData()
    }

    fun shouldSyncLuckyNumber(): Boolean {
        return (db.luckyNumberDao().getNearestFutureNow(profileId, Date.getToday().value) ?: -1) == -1
    }

    /*fun error(tag: String, errorCode: Int, response: Response? = null, throwable: Throwable? = null, apiResponse: JsonObject? = null) {
        val code = when (throwable) {
            is UnknownHostException, is SSLException, is InterruptedIOException -> CODE_NO_INTERNET
            is SocketTimeoutException -> CODE_TIMEOUT
            else -> when (response?.code()) {
                400, 401, 424, 500, 503, 404 -> CODE_MAINTENANCE
                else -> errorCode
            }
        }
        error(ApiError(tag, code).apply {
            profileId = profile?.id ?: -1
        }.withResponse(response).withThrowable(throwable).withApiResponse(apiResponse))
    }*/

    fun error(tag: String, errorCode: Int, response: Response? = null, apiResponse: String? = null) {
        error(ApiError(tag, errorCode).apply {
            profileId = profile?.id ?: -1
        }.withResponse(response).withApiResponse(apiResponse))
    }

    fun error(apiError: ApiError) {
        apiError.errorCode = when (apiError.throwable) {
            is UnknownHostException -> ERROR_REQUEST_FAILURE_HOSTNAME_NOT_FOUND
            is SSLException -> ERROR_REQUEST_FAILURE_SSL_ERROR
            is InterruptedIOException -> ERROR_REQUEST_FAILURE_NO_INTERNET
            is SocketTimeoutException -> ERROR_REQUEST_FAILURE_TIMEOUT
            else ->
                if (apiError.errorCode == ERROR_REQUEST_FAILURE)
                    when (apiError.response?.code()) {
                        400 -> ERROR_REQUEST_HTTP_400
                        401 -> ERROR_REQUEST_HTTP_401
                        403 -> ERROR_REQUEST_HTTP_403
                        404 -> ERROR_REQUEST_HTTP_404
                        405 -> ERROR_REQUEST_HTTP_405
                        410 -> ERROR_REQUEST_HTTP_410
                        424 -> ERROR_REQUEST_HTTP_424
                        500 -> ERROR_REQUEST_HTTP_500
                        503 -> ERROR_REQUEST_HTTP_503
                        else -> apiError.errorCode
                    }
                else apiError.errorCode
        }
        callback.onError(apiError)
    }

    fun progress(step: Float) {
        callback.onProgress(step)
    }

    fun startProgress(stringRes: Int) {
        callback.onStartProgress(stringRes)
    }
}
