/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_EDUDZIENNIK_WEB
import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher

/**
 * Use http://patorjk.com/software/taag/#p=display&f=Big for the ascii art
 *
 * Use https://codepen.io/kubasz/pen/RwwwbGN to easily generate the student data getters/setters
 */
class DataEdudziennik(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isWebLoginValid() = webSessionIdExpiryTime-30 > currentTimeUnix() && webSessionId.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isWebLoginValid()) {
            loginMethods += LOGIN_METHOD_EDUDZIENNIK_WEB
        }
    }

    private var mLoginEmail: String? = null
    var loginEmail: String?
        get() { mLoginEmail = mLoginEmail ?: loginStore.getLoginData("email", null); return mLoginEmail }
        set(value) { loginStore.putLoginData("email", value); mLoginEmail = value }

    private var mLoginPassword: String? = null
    var loginPassword: String?
        get() { mLoginPassword = mLoginPassword ?: loginStore.getLoginData("password", null); return mLoginPassword }
        set(value) { loginStore.putLoginData("password", value); mLoginPassword = value }

    private var mStudentId: String? = null
    var studentId: String?
        get() { mStudentId = mStudentId ?: profile?.getStudentData("studentId", null); return mStudentId }
        set(value) { profile?.putStudentData("studentId", value) ?: return; mStudentId = value }

    private var mSchoolId: String? = null
    var schoolId: String?
        get() { mSchoolId = mSchoolId ?: profile?.getStudentData("schoolId", null); return mSchoolId }
        set(value) { profile?.putStudentData("schoolId", value) ?: return; mSchoolId = value }

    private var mClassId: String? = null
    var classId: String?
        get() { mClassId = mClassId ?: profile?.getStudentData("classId", null); return mClassId }
        set(value) { profile?.putStudentData("classId", value) ?: return; mClassId = value }

    /*   __          __  _
         \ \        / / | |
          \ \  /\  / /__| |__
           \ \/  \/ / _ \ '_ \
            \  /\  /  __/ |_) |
             \/  \/ \___|_._*/
    private var mWebSessionId: String? = null
    var webSessionId: String?
        get() { mWebSessionId = mWebSessionId ?: loginStore.getLoginData("webSessionId", null); return mWebSessionId }
        set(value) { loginStore.putLoginData("webSessionId", value); mWebSessionId = value }

    private var mWebSessionIdExpiryTime: Long? = null
    var webSessionIdExpiryTime: Long
        get() { mWebSessionIdExpiryTime = mWebSessionIdExpiryTime ?: loginStore.getLoginData("webSessionIdExpiryTime", 0L); return mWebSessionIdExpiryTime ?: 0L }
        set(value) { loginStore.putLoginData("webSessionIdExpiryTime", value); mWebSessionIdExpiryTime = value }

    /*     ____  _   _
          / __ \| | | |
         | |  | | |_| |__   ___ _ __
         | |  | | __| '_ \ / _ \ '__|
         | |__| | |_| | | |  __/ |
          \____/ \__|_| |_|\___|*/
    private var mSchoolName: String? = null
    var schoolName: String?
        get() { mSchoolName = mSchoolName ?: profile?.getStudentData("schoolName", null); return mSchoolName }
        set(value) { profile?.putStudentData("schoolName", value) ?: return; mSchoolName = value }

    private var mTimetableNotPublic: Boolean? = null
    var timetableNotPublic: Boolean
        get() { mTimetableNotPublic = mTimetableNotPublic ?: profile?.getStudentData("timetableNotPublic", false); return mTimetableNotPublic ?: false }
        set(value) { profile?.putStudentData("timetableNotPublic", value) ?: return; mTimetableNotPublic = value }

    val studentEndpoint: String
        get() = "Students/$studentId/"

    val schoolEndpoint: String
        get() = "Schools/$schoolId/"

    val schoolClassEndpoint: String
        get() = "Schools/$classId/"

    val studentAndClassEndpoint: String
        get() = "Students/$studentId/Klass/$classId/"

    val courseEndpoint: String
        get() = "Course/$studentId/"

    val timetableEndpoint: String
        get() = "Plan/$studentId/"

    fun getSubject(longId: String, name: String): Subject {
        val id = longId.crc32()
        return subjectList.singleOrNull { it.id == id } ?: run {
            val subject = Subject(profileId, id, name, name)
            subjectList.put(id, subject)
            subject
        }
    }

    fun getTeacher(firstName: String, lastName: String): Teacher {
        val name = "$firstName $lastName".fixName()
        val id = name.crc32()
        return teacherList.singleOrNull { it.id == id } ?: run {
            val teacher = Teacher(profileId, id, firstName, lastName)
            teacherList.put(id, teacher)
            teacher
        }
    }

    fun getEventType(longId: String, name: String): EventType {
        val id = longId.crc16().toLong()
        return eventTypes.singleOrNull { it.id == id } ?: run {
            val eventType = EventType(profileId, id, name, colorFromName(app, name))
            eventTypes.put(id, eventType)
            eventType
        }
    }
}
