/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik

import android.util.LongSparseArray
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_MOBIDZIENNIK_WEB
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ext.currentTimeUnix
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class DataMobidziennik(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isWebLoginValid() = webSessionIdExpiryTime-30 > currentTimeUnix()
            && webSessionValue.isNotNullNorEmpty()
            && webSessionKey.isNotNullNorEmpty()
            && webServerId.isNotNullNorEmpty()

    fun isApi2LoginValid() = loginEmail.isNotNullNorEmpty()
            && loginId.isNotNullNorEmpty()
            && globalId.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isWebLoginValid()) {
            loginMethods += LOGIN_METHOD_MOBIDZIENNIK_WEB
        }
    }

    override fun generateUserCode() = "$loginServerName:$loginUsername:$studentId"

    fun parseDateTime(dateStr: String): Pair<Date, Time> {
        // pt, 4 lut, 09:11
        val dateParts = dateStr.split(',', ' ').filter { it.isNotEmpty() }
        // [pt], [4], [lut], [09:11]
        val date = Date.getToday()
        date.day = dateParts[1].toIntOrNull() ?: 1
        date.month = when (dateParts[2]) {
            "sty" -> 1
            "lut" -> 2
            "mar" -> 3
            "kwi" -> 4
            "maj" -> 5
            "cze" -> 6
            "lip" -> 7
            "sie" -> 8
            "wrz" -> 9
            "paź" -> 10
            "lis" -> 11
            "gru" -> 12
            else -> 1
        }
        val time = Time.fromH_m(dateParts[3])
        return date to time
    }

    val teachersMap = LongSparseArray<String>()
    val subjectsMap = LongSparseArray<String>()

    val gradeAddedDates = sortedMapOf<Long, Long>()
    val gradeAverages = sortedMapOf<Long, Float>()
    val gradeColors = sortedMapOf<Long, Int>()

    private var mLoginServerName: String? = null
    var loginServerName: String?
        get() { mLoginServerName = mLoginServerName ?: loginStore.getLoginData("serverName", null); return mLoginServerName }
        set(value) { loginStore.putLoginData("serverName", value); mLoginServerName = value }

    private var mLoginUsername: String? = null
    var loginUsername: String?
        get() { mLoginUsername = mLoginUsername ?: loginStore.getLoginData("username", null); return mLoginUsername }
        set(value) { loginStore.putLoginData("username", value); mLoginUsername = value }

    private var mLoginPassword: String? = null
    var loginPassword: String?
        get() { mLoginPassword = mLoginPassword ?: loginStore.getLoginData("password", null); return mLoginPassword }
        set(value) { loginStore.putLoginData("password", value); mLoginPassword = value }

    private var mStudentId: Int? = null
    var studentId: Int
        get() { mStudentId = mStudentId ?: profile?.getStudentData("studentId", 0); return mStudentId ?: 0 }
        set(value) { profile?.putStudentData("studentId", value) ?: return; mStudentId = value }

    /*   __          __  _
         \ \        / / | |
          \ \  /\  / /__| |__
           \ \/  \/ / _ \ '_ \
            \  /\  /  __/ |_) |
             \/  \/ \___|_._*/
    private var mWebSessionKey: String? = null
    var webSessionKey: String?
        get() { mWebSessionKey = mWebSessionKey ?: loginStore.getLoginData("sessionCookie", null); return mWebSessionKey }
        set(value) { loginStore.putLoginData("sessionCookie", value); mWebSessionKey = value }

    private var mWebSessionValue: String? = null
    var webSessionValue: String?
        get() { mWebSessionValue = mWebSessionValue ?: loginStore.getLoginData("sessionID", null); return mWebSessionValue }
        set(value) { loginStore.putLoginData("sessionID", value); mWebSessionValue = value }

    private var mWebServerId: String? = null
    var webServerId: String?
        get() { mWebServerId = mWebServerId ?: loginStore.getLoginData("sessionServer", null); return mWebServerId }
        set(value) { loginStore.putLoginData("sessionServer", value); mWebServerId = value }

    private var mWebSessionIdExpiryTime: Long? = null
    var webSessionIdExpiryTime: Long
        get() { mWebSessionIdExpiryTime = mWebSessionIdExpiryTime ?: loginStore.getLoginData("sessionIDTime", 0L); return mWebSessionIdExpiryTime ?: 0L }
        set(value) { loginStore.putLoginData("sessionIDTime", value); mWebSessionIdExpiryTime = value }

    /*             _____ _____   ___
             /\   |  __ \_   _| |__ \
            /  \  | |__) || |      ) |
           / /\ \ |  ___/ | |     / /
          / ____ \| |    _| |_   / /_
         /_/    \_\_|   |_____| |___*/
    /**
     * A global ID (whatever it is) used in API 2
     * and Firebase push from Mobidziennik.
     */
    var globalId: String?
        get() { mGlobalId = mGlobalId ?: profile?.getStudentData("globalId", null); return mGlobalId }
        set(value) { profile?.putStudentData("globalId", value) ?: return; mGlobalId = value }
    private var mGlobalId: String? = null

    /**
     * User's email that may or may not
     * be retrieved from Web by [MobidziennikWebAccountEmail].
     * Used to log in to API 2.
     */
    var loginEmail: String?
        get() { mLoginEmail = mLoginEmail ?: profile?.getStudentData("email", null); return mLoginEmail }
        set(value) { profile?.putStudentData("email", value); mLoginEmail = value }
    private var mLoginEmail: String? = null

    /**
     * A login ID used in the API 2.
     * Looks more or less like "7063@2019@zslpoznan".
     */
    var loginId: String?
        get() { mLoginId = mLoginId ?: profile?.getStudentData("loginId", null); return mLoginId }
        set(value) { profile?.putStudentData("loginId", value) ?: return; mLoginId = value }
    private var mLoginId: String? = null

    /**
     * No need to explain.
     */
    var ciasteczkoAutoryzacji: String?
        get() { mCiasteczkoAutoryzacji = mCiasteczkoAutoryzacji ?: profile?.getStudentData("ciasteczkoAutoryzacji", null); return mCiasteczkoAutoryzacji }
        set(value) { profile?.putStudentData("ciasteczkoAutoryzacji", value) ?: return; mCiasteczkoAutoryzacji = value }
    private var mCiasteczkoAutoryzacji: String? = null


    override fun saveData() {
        super.saveData()
        if (gradeAddedDates.isNotEmpty()) {
            app.db.gradeDao().updateDetails(profileId, gradeAverages, gradeAddedDates, gradeColors)
        }
    }

    val mobiLessons = mutableListOf<MobiLesson>()

    data class MobiLesson(
            var id: Long,
            var subjectId: Long,
            var teacherId: Long,
            var teamId: Long,
            var topic: String,
            var date: Date,
            var startTime: Time,
            var endTime: Time,
            var presentCount: Int,
            var absentCount: Int,
            var lessonNumber: Int,
            var signed: String
    )
}
