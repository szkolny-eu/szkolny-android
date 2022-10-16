package pl.szczodrzynski.edziennik.data.api.models

import android.os.Bundle
import android.util.LongSparseArray
import android.util.SparseArray
import androidx.core.util.set
import androidx.core.util.size
import androidx.room.OnConflictStrategy
import com.google.gson.JsonObject
import im.wangchao.mhttp.Response
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.ERROR_REQUEST_FAILURE
import pl.szczodrzynski.edziennik.data.api.Regexes.MESSAGE_META
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

abstract class Data(val app: App, val profile: Profile?, val loginStore: LoginStore) {
    companion object {
        private const val TAG = "Data"
        private val DEBUG = true && BuildConfig.DEBUG
    }

    var fakeLogin = false

    var cancelled = false

    val profileId
        get() = profile?.id ?: -1

    var arguments: JsonObject? = null

    /**
     * A callback passed to all [Feature]s and [LoginMethod]s
     */
    lateinit var callback: EdziennikCallback

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
     * A map of endpoint ID to last sync time, that are still pending
     * to run.
     */
    var targetEndpointIds = sortedMapOf<Int, Long?>()
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
    var eventListReplace = false
    var messageListReplace = false
    var announcementListReplace = false

    val classrooms = LongSparseArray<Classroom>()
    val attendanceTypes = LongSparseArray<AttendanceType>()
    val noticeTypes = LongSparseArray<NoticeType>()
    val eventTypes = LongSparseArray<EventType>()
    val teacherAbsenceTypes = LongSparseArray<TeacherAbsenceType>()
    val librusLessons = LongSparseArray<LibrusLesson>()

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

    val luckyNumberList = mutableListOf<LuckyNumber>()

    val teacherAbsenceList = mutableListOf<TeacherAbsence>()

    val messageList = mutableListOf<Message>()
    val messageRecipientList = mutableListOf<MessageRecipient>()
    val messageRecipientIgnoreList = mutableListOf<MessageRecipient>()

    val metadataList = mutableListOf<Metadata>()
    val setSeenMetadataList = mutableListOf<Metadata>()

    val db: AppDb by lazy { app.db }

    init {
        if (App.debugMode) {
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

    private fun d(message: String) {
        if (DEBUG)
            Utils.d(TAG, message)
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
        librusLessons.clear()

        lessonList.clear()
        gradeList.clear()
        noticeList.clear()
        attendanceList.clear()
        announcementList.clear()
        luckyNumberList.clear()
        teacherAbsenceList.clear()
        messageList.clear()
        messageRecipientList.clear()
        messageRecipientIgnoreList.clear()
        metadataList.clear()
        setSeenMetadataList.clear()
    }

    open fun saveData() {
        if (profile == null)
            return // return on first login
        val totalStart = System.currentTimeMillis()
        var startTime = System.currentTimeMillis()
        d("Saving data to DB")

        profile.userCode = generateUserCode()

        // update profile subname with class name, school year and account type
        profile.subname = joinNotNullStrings(
                " - ",
                profile.studentClassName,
                "${profile.studentSchoolYearStart}/${profile.studentSchoolYearStart + 1}"
        ) + " " + app.getString(if (profile.isParent) R.string.account_type_parent else R.string.account_type_child)

        db.profileDao().add(profile)
        db.loginStoreDao().add(loginStore)

        if (profile.id == app.profile.id) {
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

        d("Profiles saved in ${System.currentTimeMillis()-startTime} ms")
        startTime = System.currentTimeMillis()

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

        d("Maps saved in ${System.currentTimeMillis()-startTime} ms")
        startTime = System.currentTimeMillis()

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
        if (librusLessons.size > 0)
            db.librusLessonDao().addAll(librusLessons.values())

        d("On-demand maps saved in ${System.currentTimeMillis()-startTime} ms")
        startTime = System.currentTimeMillis()

        // clear DB with DataRemoveModels added by endpoints
        for (model in toRemove) {
            d("Clearing DB with $model")
            when (model) {
                is DataRemoveModel.Timetable -> model.commit(profileId, db.timetableDao())
                is DataRemoveModel.Grades -> model.commit(profileId, db.gradeDao())
                is DataRemoveModel.Events -> model.commit(profileId, db.eventDao())
                is DataRemoveModel.Attendance -> model.commit(profileId, db.attendanceDao())
            }
        }

        d("DB cleared in ${System.currentTimeMillis()-startTime} ms")
        startTime = System.currentTimeMillis()

        if (metadataList.isNotEmpty())
            db.metadataDao().addAllIgnore(metadataList)
        if (setSeenMetadataList.isNotEmpty())
            db.metadataDao().setSeen(setSeenMetadataList)

        d("Metadata saved in ${System.currentTimeMillis()-startTime} ms")
        startTime = System.currentTimeMillis()

        db.timetableDao().putAll(lessonList, removeNotKept = true)
        db.gradeDao().putAll(gradeList, removeNotKept = true)
        db.eventDao().putAll(eventList, forceReplace = eventListReplace, removeNotKept = true)
        if (noticeList.isNotEmpty()) {
            db.noticeDao().clear(profile.id)
            db.noticeDao().putAll(noticeList)
        }
        db.attendanceDao().putAll(attendanceList, removeNotKept = true)
        db.announcementDao().putAll(announcementList, forceReplace = announcementListReplace, removeNotKept = false)
        db.luckyNumberDao().putAll(luckyNumberList)
        db.teacherAbsenceDao().putAll(teacherAbsenceList)

        db.messageDao().putAll(messageList, forceReplace = messageListReplace, removeNotKept = false)
        if (messageRecipientList.isNotEmpty())
            db.messageRecipientDao().addAll(messageRecipientList)
        if (messageRecipientIgnoreList.isNotEmpty())
            db.messageRecipientDao().addAllIgnore(messageRecipientIgnoreList)

        d("Other data saved in ${System.currentTimeMillis()-startTime} ms")

        d("Total save time: ${System.currentTimeMillis()-totalStart} ms")
    }

    fun setSyncNext(endpointId: Int, syncIn: Long? = null, viewId: Int? = null, syncAt: Long? = null) {
        EndpointTimer(profile?.id
                ?: -1, endpointId).apply {
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
        d("Cancelled")
        cancelled = true
        saveData()
    }

    fun shouldSyncLuckyNumber(): Boolean {
        return db.luckyNumberDao().getNearestFutureNow(profileId, Date.getToday()) == null
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
        apiError.errorCode = apiError.throwable?.toErrorCode() ?:
                if (apiError.errorCode == ERROR_REQUEST_FAILURE)
                    apiError.response?.toErrorCode() ?: apiError.errorCode
                else
                    apiError.errorCode

        callback.onError(apiError)
    }

    fun requireUserAction(type: UserActionRequiredEvent.Type, params: Bundle, errorText: Int) {
        callback.onRequiresUserAction(UserActionRequiredEvent(
            profileId = profile?.id,
            type = type,
            params = params,
            errorText = errorText,
        ))
    }

    fun progress(step: Float) {
        callback.onProgress(step)
    }

    fun startProgress(stringRes: Int) {
        callback.onStartProgress(stringRes)
    }

    /*    _    _ _   _ _
         | |  | | | (_) |
         | |  | | |_ _| |___
         | |  | | __| | / __|
         | |__| | |_| | \__ \
          \____/ \__|_|_|__*/
    fun getSubject(id: Long?, name: String, shortName: String = name): Subject {
        var subject = subjectList.singleOrNull { it.id == id }
        if (subject == null)
            subject = subjectList.singleOrNull { it.longName == name }
        if (subject == null)
            subject = subjectList.singleOrNull { it.shortName == name }

        if (subject == null) {
            subject = Subject(
                profileId,
                id ?: name.crc32(),
                name,
                shortName
            )
            subjectList[subject.id] = subject
        }
        return subject
    }

    fun getTeam(
        id: Long?,
        name: String,
        schoolCode: String,
        isTeamClass: Boolean = false,
        profileId: Int? = null,
    ): Team {
        if (isTeamClass && teamClass != null)
            return teamClass as Team
        var team = teamList.singleOrNull { it.id == id }

        val namePlain = name.replace(" ", "")
        if (team == null)
            team = teamList.singleOrNull { it.name.replace(" ", "") == namePlain }

        if (team == null) {
            team = Team(
                profileId ?: this.profileId,
                id ?: name.crc32(),
                name,
                if (isTeamClass) Team.TYPE_CLASS else Team.TYPE_VIRTUAL,
                "$schoolCode:$name",
                -1
            )
            teamList[team.id] = team
        } else if (id != null) {
            team.id = id
        }
        return team
    }

    fun getTeacher(firstName: String, lastName: String, loginId: String? = null, id: Long? = null): Teacher {
        val teacher = teacherList.singleOrNull { it.fullName == "$firstName $lastName" }
        return validateTeacher(teacher, firstName, lastName, loginId, id)
    }

    fun getTeacher(firstNameChar: Char, lastName: String, loginId: String? = null): Teacher {
        val teacher = teacherList.singleOrNull { it.shortName == "$firstNameChar.$lastName" }
        return validateTeacher(teacher, firstNameChar.toString(), lastName, loginId, null)
    }

    fun getTeacherByLastFirst(nameLastFirst: String, loginId: String? = null): Teacher {
        // comparing full name is safer than splitting and swapping
        val teacher = teacherList.singleOrNull { it.fullNameLastFirst == nameLastFirst }
        val nameParts = nameLastFirst.split(" ", limit = 2)
        return if (nameParts.size == 1)
            validateTeacher(teacher, nameParts[0], "", loginId, null)
        else
            validateTeacher(teacher, nameParts[1], nameParts[0], loginId, null)
    }

    fun getTeacherByFirstLast(nameFirstLast: String, loginId: String? = null): Teacher {
        // comparing full name is safer than splitting and swapping
        val teacher = teacherList.singleOrNull { it.fullName == nameFirstLast }
        val nameParts = nameFirstLast.split(" ", limit = 2)
        return if (nameParts.size == 1)
            validateTeacher(teacher, nameParts[0], "", loginId, null)
        else
            validateTeacher(teacher, nameParts[0], nameParts[1], loginId, null)
    }

    fun getTeacherByFDotLast(nameFDotLast: String, loginId: String? = null): Teacher {
        val nameParts = nameFDotLast.split(".")
        return if (nameParts.size == 1)
            getTeacher(nameParts[0], "", loginId)
        else
            getTeacher(nameParts[0][0], nameParts[1], loginId)
    }

    fun getTeacherByFDotSpaceLast(nameFDotSpaceLast: String, loginId: String? = null): Teacher {
        val nameParts = nameFDotSpaceLast.split(".")
        return if (nameParts.size == 1)
            getTeacher(nameParts[0], "", loginId)
        else
            getTeacher(nameParts[0][0], nameParts[1], loginId)
    }

    private fun validateTeacher(
        teacher: Teacher?,
        firstName: String,
        lastName: String,
        loginId: String?,
        id: Long?
    ): Teacher {
        val obj = teacher ?: Teacher(profileId, -1, firstName, lastName, loginId).also {
            it.id = id ?: it.fullName.crc32()
            teacherList[it.id] = it
        }
        return obj.also {
            if (loginId != null)
                it.loginId = loginId
            if (firstName.length > 1)
                it.name = firstName
            it.surname = lastName
        }
    }

    fun parseMessageMeta(body: String): Map<String, String>? {
        val match = MESSAGE_META.find(body) ?: return null
        return match[1].split("&").associateBy(
            { it.substringBefore("=") },
            { it.substringAfter("=") },
        )
    }
}
