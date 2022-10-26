/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-12
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.enums.LoginMethod
import pl.szczodrzynski.edziennik.ext.crc32
import pl.szczodrzynski.edziennik.ext.getStudentData
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.ext.set

class DataPodlasie(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isApiLoginValid() = apiToken.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isApiLoginValid())
            loginMethods += LoginMethod.PODLASIE_API
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
        set(value) { profile["apiUrl"] = value; mApiUrl = value }

    /*     ____  _   _
          / __ \| | | |
         | |  | | |_| |__   ___ _ __
         | |  | | __| '_ \ / _ \ '__|
         | |__| | |_| | | |  __/ |
          \____/ \__|_| |_|\___|*/
    private var mStudentId: String? = null
    var studentId: String?
        get() { mStudentId = mStudentId ?: profile?.getStudentData("studentId", null); return mStudentId }
        set(value) { profile["studentId"] = value; mStudentId = value }

    private var mStudentLogin: String? = null
    var studentLogin: String?
        get() { mStudentLogin = mStudentLogin ?: profile?.getStudentData("studentLogin", null); return mStudentLogin }
        set(value) { profile["studentLogin"] = value; mStudentLogin = value }

    private var mSchoolName: String? = null
    var schoolName: String?
        get() { mSchoolName = mSchoolName ?: profile?.getStudentData("schoolName", null); return mSchoolName }
        set(value) { profile["schoolName"] = value; mSchoolName = value }

    private var mClassName: String? = null
    var className: String?
        get() { mClassName = mClassName ?: profile?.getStudentData("className", null); return mClassName }
        set(value) { profile["className"] = value; mClassName = value }

    private var mSchoolYear: String? = null
    var schoolYear: String?
        get() { mSchoolYear = mSchoolYear ?: profile?.getStudentData("schoolYear", null); return mSchoolYear }
        set(value) { profile["schoolYear"] = value; mSchoolYear = value }

    private var mCurrentSemester: Int? = null
    var currentSemester: Int
        get() { mCurrentSemester = mCurrentSemester ?: profile?.getStudentData("currentSemester", 0); return mCurrentSemester ?: 0 }
        set(value) { profile["currentSemester"] = value; mCurrentSemester = value }

    val schoolShortName: String?
        get() = studentLogin?.split('@')?.get(1)?.replace(".podlaskie.pl", "")

    val loginShort: String?
        get() = studentLogin?.split('@')?.get(0)
}
