package pl.szczodrzynski.edziennik.api.v2.models

import android.util.LongSparseArray
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty
import com.google.gson.JsonObject
import im.wangchao.mhttp.Response
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.interfaces.EndpointCallback
import pl.szczodrzynski.edziennik.data.api.AppError.*
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
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.toSparseArray
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.values
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

open class Data(val app: App, val profile: Profile?, val loginStore: LoginStore) {

    var fakeLogin = false

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

    /**
     * A list of per-endpoint next sync time descriptors.
     *
     * [EndpointTimer.nextSync] may be:
     * - [pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_NEVER] to never sync the endpoint (pretty useless)
     * - [pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS] to sync the endpoint during every sync
     * - [pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_IF_EXPLICIT] to sync the endpoint only if the matching
     *      feature ID is in the input set
     * - [pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_IF_EXPLICIT_OR_ALL] to sync if the matching feature ID
     *      is in the input set OR the sync covers all feature IDs
     * - a Unix-epoch timestamp (in millis) to sync the endpoint if [System.currentTimeMillis] is greater or equal to this value
     */
    var endpointTimers = mutableListOf<EndpointTimer>()

    val teacherList = LongSparseArray<Teacher>()
    val subjectList = LongSparseArray<Subject>()
    val teamList = LongSparseArray<Team>()
    val lessonRanges = SparseArray<LessonRange>()

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
    val gradeCategoryList = mutableListOf<GradeCategory>()
    val gradeList = mutableListOf<Grade>()

    var eventsToRemove: DataRemoveModel? = null
    val eventList = mutableListOf<Event>()
    val eventTypeList = mutableListOf<EventType>()

    var noticesToRemove: DataRemoveModel? = null
    val noticeList = mutableListOf<Notice>()

    var attendanceToRemove: DataRemoveModel? = null
    val attendanceList = mutableListOf<Attendance>()

    var announcementsToRemove: DataRemoveModel? = null
    val announcementList = mutableListOf<Announcement>()

    val messageList = mutableListOf<Message>()
    val messageRecipientList = mutableListOf<MessageRecipient>()
    val messageRecipientIgnoreList = mutableListOf<MessageRecipient>()

    val metadataList = mutableListOf<Metadata>()
    val messageMetadataList = mutableListOf<Metadata>()

    private val db by lazy { app.db }

    init {

        clear()

        if (profile != null) {
            endpointTimers = db.endpointTimerDao().getAllNow(profile.id).toMutableList()
            db.teacherDao().getAllNow(profileId).toSparseArray(teacherList) { it.id }
            db.subjectDao().getAllNow(profileId).toSparseArray(subjectList) { it.id }
            db.teamDao().getAllNow(profileId).toSparseArray(teamList) { it.id }
            db.lessonRangeDao().getAllNow(profileId).toSparseArray(lessonRanges) { it.lessonNumber }
        }

        /*val teacher = teachers.byNameFirstLast("Jan Kowalski") ?: Teacher(1, 1, "", "").let {
            teachers.add(it)
        }*/

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
        gradeCategoryList.clear()
        gradeList.clear()
        eventTypeList.clear()
        noticeList.clear()
        attendanceList.clear()
        announcementList.clear()
        messageList.clear()
        messageRecipientList.clear()
        messageRecipientIgnoreList.clear()
        metadataList.clear()
        messageMetadataList.clear()
    }

    fun saveData() {
        if (profile == null)
            return

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

        if (lessonList.isNotEmpty()) {
            db.lessonDao().clear(profile.id)
            db.lessonDao().addAll(lessonList)
        }
        if (lessonChangeList.isNotEmpty())
            db.lessonChangeDao().addAll(lessonChangeList)
        if (gradeCategoryList.isNotEmpty())
            db.gradeCategoryDao().addAll(gradeCategoryList)
        if (gradeList.isNotEmpty()) {
            db.gradeDao().clear(profile.id)
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

    fun error(tag: String, errorCode: Int, response: Response? = null, throwable: Throwable? = null, apiResponse: JsonObject? = null) {
        var code = when (throwable) {
            is UnknownHostException, is SSLException, is InterruptedIOException -> CODE_NO_INTERNET
            is SocketTimeoutException -> CODE_TIMEOUT
            else -> when (response?.code()) {
                400, 401, 424, 500, 503, 404 -> CODE_MAINTENANCE
                else -> errorCode
            }
        }
        callback.onError(ApiError(tag, code).apply { profileId = profile?.id ?: -1 }.withResponse(response).withThrowable(throwable).withApiResponse(apiResponse))
    }
    fun error(tag: String, errorCode: Int, response: Response? = null, apiResponse: String? = null) {
        var code = when (null) {
            is UnknownHostException, is SSLException, is InterruptedIOException -> CODE_NO_INTERNET
            is SocketTimeoutException -> CODE_TIMEOUT
            else -> when (response?.code()) {
                400, 401, 424, 500, 503, 404 -> CODE_MAINTENANCE
                else -> errorCode
            }
        }
        callback.onError(ApiError(tag, code).apply { profileId = profile?.id ?: -1 }.withResponse(response).withApiResponse(apiResponse))
    }
    fun error(apiError: ApiError) {
        callback.onError(apiError)
    }
    fun progress(step: Int) {
        callback.onProgress(step)
    }
    fun startProgress(stringRes: Int) {
        callback.onStartProgress(stringRes)
    }
}