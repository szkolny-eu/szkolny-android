package pl.szczodrzynski.edziennik.api.v2.models

import android.util.LongSparseArray
import android.util.SparseArray
import com.google.gson.JsonObject
import im.wangchao.mhttp.Response
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.DataNotifications
import pl.szczodrzynski.edziennik.api.v2.EXCEPTION_NOTIFY_AND_SYNC
import pl.szczodrzynski.edziennik.api.v2.ServerSync
import pl.szczodrzynski.edziennik.api.v2.interfaces.EndpointCallback
import pl.szczodrzynski.edziennik.data.api.AppError.*
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.modules.announcements.Announcement
import pl.szczodrzynski.edziennik.data.db.modules.api.EndpointTimer
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory
import pl.szczodrzynski.edziennik.data.db.modules.lessons.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonRange
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.luckynumber.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.toSparseArray
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.values
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

open class Data(val app: App, val profile: Profile?, val loginStore: LoginStore) {
    companion object {
        private const val TAG = "Data"
    }

    var fakeLogin = false

    var cancelled = false

    val profileId
        get() = profile?.id ?: -1

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
    val attendanceTypes = SparseArray<Pair<Int, String>>()

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

    var lessonsToRemove: DataRemoveModel? = null
    val lessonList = mutableListOf<Lesson>()
    val lessonChangeList = mutableListOf<LessonChange>()

    var gradesToRemove: DataRemoveModel? = null
    val gradeList = mutableListOf<Grade>()

    var eventsToRemove: DataRemoveModel? = null
    val eventList = mutableListOf<Event>()
    val eventTypeList = mutableListOf<EventType>()

    var noticesToRemove: DataRemoveModel? = null
    val noticeList = mutableListOf<Notice>()

    var attendancesToRemove: DataRemoveModel? = null
    val attendanceList = mutableListOf<Attendance>()

    var announcementsToRemove: DataRemoveModel? = null
    val announcementList = mutableListOf<Announcement>()

    val luckyNumberList = mutableListOf<LuckyNumber>()

    val messageList = mutableListOf<Message>()
    val messageRecipientList = mutableListOf<MessageRecipient>()
    val messageRecipientIgnoreList = mutableListOf<MessageRecipient>()

    val metadataList = mutableListOf<Metadata>()
    val messageMetadataList = mutableListOf<Metadata>()

    val db: AppDb by lazy { app.db }

    init {
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

        endpointTimers.clear()
        teacherList.clear()
        subjectList.clear()
        teamList.clear()
        lessonRanges.clear()

        lessonList.clear()
        lessonChangeList.clear()
        gradeCategories.clear()
        gradeList.clear()
        eventTypeList.clear()
        noticeList.clear()
        attendanceList.clear()
        announcementList.clear()
        luckyNumberList.clear()
        messageList.clear()
        messageRecipientList.clear()
        messageRecipientIgnoreList.clear()
        metadataList.clear()
        messageMetadataList.clear()
    }

    open fun saveData() {
        if (profile == null)
            return // return on first login

        profile.empty = false

        db.profileDao().add(profile)
        db.loginStoreDao().add(loginStore)

        db.endpointTimerDao().addAll(endpointTimers)
        db.teacherDao().clear(profileId)
        db.teacherDao().addAll(teacherList.values())
        db.subjectDao().clear(profileId)
        db.subjectDao().addAll(subjectList.values())
        db.teamDao().clear(profileId)
        db.teamDao().addAll(teamList.values())
        db.lessonRangeDao().clear(profileId)
        db.lessonRangeDao().addAll(lessonRanges.values())
        db.gradeCategoryDao().clear(profileId)
        db.gradeCategoryDao().addAll(gradeCategories.values())

        gradesToRemove?.let { it ->
            it.removeAll?.let { _ -> db.gradeDao().clear(profileId) }
            it.removeSemester?.let { semester -> db.gradeDao().clearForSemester(profileId, semester) }
        }

        if (lessonList.isNotEmpty()) {
            db.lessonDao().clear(profile.id)
            db.lessonDao().addAll(lessonList)
        }
        if (lessonChangeList.isNotEmpty())
            db.lessonChangeDao().addAll(lessonChangeList)
        if (gradeList.isNotEmpty()) {
            db.gradeDao().addAll(gradeList)
        }
        if (eventList.isNotEmpty()) {
            db.eventDao().removeFuture(profile.id, Date.getToday())
            db.eventDao().addAll(eventList)
        }
        if (eventTypeList.isNotEmpty())
            db.eventTypeDao().addAll(eventTypeList)
        if (noticeList.isNotEmpty()) {
            db.noticeDao().clear(profile.id)
            db.noticeDao().addAll(noticeList)
        }
        if (attendanceList.isNotEmpty())
            db.attendanceDao().addAll(attendanceList)
        if (announcementList.isNotEmpty())
            db.announcementDao().addAll(announcementList)
        if (luckyNumberList.isNotEmpty())
            db.luckyNumberDao().addAll(luckyNumberList)

        if (messageList.isNotEmpty())
            db.messageDao().addAllIgnore(messageList)
        if (messageRecipientList.isNotEmpty())
            db.messageRecipientDao().addAll(messageRecipientList)
        if (messageRecipientIgnoreList.isNotEmpty())
            db.messageRecipientDao().addAllIgnore(messageRecipientIgnoreList)
        if (metadataList.isNotEmpty())
            db.metadataDao().addAllIgnore(metadataList)
        if (messageMetadataList.isNotEmpty())
            db.metadataDao().setSeen(messageMetadataList)
    }

    fun notifyAndSyncEvents(onSuccess: () -> Unit) {
        try {
            DataNotifications(this)
            ServerSync(this) {
                db.notificationDao().addAll(notifications)
                onSuccess()
            }
        }
        catch (e: Exception) {
            error(ApiError(TAG, EXCEPTION_NOTIFY_AND_SYNC)
                    .withThrowable(e))
        }
    }

    fun setSyncNext(endpointId: Int, syncIn: Long? = null, viewId: Int? = null) {
        EndpointTimer(profile?.id ?: -1, endpointId).apply {
            syncedNow()

            if (syncIn != null) {
                if (syncIn < 10)
                    nextSync = syncIn
                else
                    syncIn(syncIn)
            }
            if (viewId != null)
                syncWhenView(viewId)

            endpointTimers.add(this)
        }
    }

    fun cancel() {
        d("Data", "Cancelled")
        cancelled = true
        saveData()
    }

    fun error(tag: String, errorCode: Int, response: Response? = null, throwable: Throwable? = null, apiResponse: JsonObject? = null) {
        val code = when (throwable) {
            is UnknownHostException, is SSLException, is InterruptedIOException -> CODE_NO_INTERNET
            is SocketTimeoutException -> CODE_TIMEOUT
            else -> when (response?.code()) {
                400, 401, 424, 500, 503, 404 -> CODE_MAINTENANCE
                else -> errorCode
            }
        }
        error(ApiError(tag, code).apply { profileId = profile?.id ?: -1 }.withResponse(response).withThrowable(throwable).withApiResponse(apiResponse))
    }
    fun error(tag: String, errorCode: Int, response: Response? = null, apiResponse: String? = null) {
        val code = when (null) {
            is UnknownHostException, is SSLException, is InterruptedIOException -> CODE_NO_INTERNET
            is SocketTimeoutException -> CODE_TIMEOUT
            else -> when (response?.code()) {
                400, 401, 424, 500, 503, 404 -> CODE_MAINTENANCE
                else -> errorCode
            }
        }
        error(ApiError(tag, code).apply { profileId = profile?.id ?: -1 }.withResponse(response).withApiResponse(apiResponse))
    }
    fun error(apiError: ApiError) {
        if (apiError.isCritical)
            cancel()
        callback.onError(apiError)
    }
    fun progress(step: Int) {
        callback.onProgress(step)
    }
    fun startProgress(stringRes: Int) {
        callback.onStartProgress(stringRes)
    }
}
