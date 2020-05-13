/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-12
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_PODLASIE_API
import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.db.entity.*
import kotlin.text.replace

class DataPodlasie(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isApiLoginValid() = apiToken.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isApiLoginValid())
            loginMethods += LOGIN_METHOD_PODLASIE_API
    }

    override fun generateUserCode(): String = "$schoolShortName:$loginShort:${studentId?.crc32()}"

    /*                   _
             /\         (_)
            /  \   _ __  _
           / /\ \ | '_ \| |
          / ____ \| |_) | |
         /_/    \_\ .__/|_|
                  | |
                  |*/
    private var mApiToken: String? = null
    var apiToken: String?
        get() { mApiToken = mApiToken ?: loginStore.getLoginData("apiToken", null); return mApiToken }
        set(value) { loginStore.putLoginData("apiToken", value); mApiToken = value }

    private var mApiUrl: String? = null
    var apiUrl: String?
        get() { mApiUrl = mApiUrl ?: profile?.getStudentData("apiUrl", null); return mApiUrl }
        set(value) { profile?.putStudentData("apiUrl", value) ?: return; mApiUrl = value }

    /*     ____  _   _
          / __ \| | | |
         | |  | | |_| |__   ___ _ __
         | |  | | __| '_ \ / _ \ '__|
         | |__| | |_| | | |  __/ |
          \____/ \__|_| |_|\___|*/
    private var mStudentId: String? = null
    var studentId: String?
        get() { mStudentId = mStudentId ?: profile?.getStudentData("studentId", null); return mStudentId }
        set(value) { profile?.putStudentData("studentId", value) ?: return; mStudentId = value }

    private var mStudentLogin: String? = null
    var studentLogin: String?
        get() { mStudentLogin = mStudentLogin ?: profile?.getStudentData("studentLogin", null); return mStudentLogin }
        set(value) { profile?.putStudentData("studentLogin", value) ?: return; mStudentLogin = value }

    private var mSchoolName: String? = null
    var schoolName: String?
        get() { mSchoolName = mSchoolName ?: profile?.getStudentData("schoolName", null); return mSchoolName }
        set(value) { profile?.putStudentData("schoolName", value) ?: return; mSchoolName = value }

    private var mClassName: String? = null
    var className: String?
        get() { mClassName = mClassName ?: profile?.getStudentData("className", null); return mClassName }
        set(value) { profile?.putStudentData("className", value) ?: return; mClassName = value }

    private var mSchoolYear: String? = null
    var schoolYear: String?
        get() { mSchoolYear = mSchoolYear ?: profile?.getStudentData("schoolYear", null); return mSchoolYear }
        set(value) { profile?.putStudentData("schoolYear", value) ?: return; mSchoolYear = value }

    private var mCurrentSemester: Int? = null
    var currentSemester: Int
        get() { mCurrentSemester = mCurrentSemester ?: profile?.getStudentData("currentSemester", 0); return mCurrentSemester ?: 0 }
        set(value) { profile?.putStudentData("currentSemester", value) ?: return; mCurrentSemester = value }

    val schoolShortName: String?
        get() = studentLogin?.split('@')?.get(1)?.replace(".podlaskie.pl", "")

    val loginShort: String?
        get() = studentLogin?.split('@')?.get(0)

    fun getSubject(name: String): Subject {
        val id = name.crc32()
        return subjectList.singleOrNull { it.id == id } ?: run {
            val subject = Subject(profileId, id, name, name)
            subjectList.put(id, subject)
            subject
        }
    }

    fun getTeacher(firstName: String, lastName: String): Teacher {
        val name = "$firstName $lastName".fixName()
        return teacherList.singleOrNull { it.fullName == name } ?: run {
            val id = name.crc32()
            val teacher = Teacher(profileId, id, firstName, lastName)
            teacherList.put(id, teacher)
            teacher
        }
    }

    fun getTeam(name: String? = null): Team {
        if (name == "cała klasa" || name == null) return teamClass ?: run {
            val id = className!!.crc32()
            val teamCode = "$schoolShortName:$className"
            val team = Team(profileId, id, className, Team.TYPE_CLASS, teamCode, -1)
            teamList.put(id, team)
            return team
        } else {
            val id = name.crc32()
            val teamCode = "$schoolShortName:$name"
            val team = Team(profileId, id, name, Team.TYPE_VIRTUAL, teamCode, -1)
            teamList.put(id, team)
            return team
        }
    }
}
