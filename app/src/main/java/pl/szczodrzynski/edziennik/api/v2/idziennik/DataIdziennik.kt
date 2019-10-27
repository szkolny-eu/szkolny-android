/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-25. 
 */

package pl.szczodrzynski.edziennik.api.v2.idziennik

import okhttp3.Cookie
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_IDZIENNIK_API
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_IDZIENNIK_WEB
import pl.szczodrzynski.edziennik.api.v2.models.Data
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.isNotNullNorEmpty

class DataIdziennik(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isWebLoginValid() = loginExpiryTime-30 > currentTimeUnix() && webSessionId.isNotNullNorEmpty() && webAuth.isNotNullNorEmpty()
    fun isApiLoginValid() = loginExpiryTime-30 > currentTimeUnix() && apiBearer.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isWebLoginValid()) {
            loginMethods += LOGIN_METHOD_IDZIENNIK_WEB
            app.cookieJar.saveFromResponse(null, listOf(
                    Cookie.Builder()
                            .name("ASP.NET_SessionId_iDziennik")
                            .value(webSessionId!!)
                            .domain("iuczniowie.progman.pl")
                            .secure().httpOnly().build(),
                    Cookie.Builder()
                            .name(".ASPXAUTH")
                            .value(webAuth!!)
                            .domain("iuczniowie.progman.pl")
                            .secure().httpOnly().build()
            ))
        }
        if (isApiLoginValid())
            loginMethods += LOGIN_METHOD_IDZIENNIK_API
    }

    private var mLoginExpiryTime: Long? = null
    var loginExpiryTime: Long
        get() { mLoginExpiryTime = mLoginExpiryTime ?: loginStore.getLoginData("loginExpiryTime", 0L); return mLoginExpiryTime ?: 0L }
        set(value) { loginStore.putLoginData("loginExpiryTime", value); mLoginExpiryTime = value }

    /*   __          __  _
         \ \        / / | |
          \ \  /\  / /__| |__
           \ \/  \/ / _ \ '_ \
            \  /\  /  __/ |_) |
             \/  \/ \___|_._*/
    private var mWebSchoolName: String? = null
    var webSchoolName: String?
        get() { mWebSchoolName = mWebSchoolName ?: loginStore.getLoginData("schoolName", null); return mWebSchoolName }
        set(value) { loginStore.putLoginData("schoolName", value); mWebSchoolName = value }
    private var mWebUsername: String? = null
    var webUsername: String?
        get() { mWebUsername = mWebUsername ?: loginStore.getLoginData("username", null); return mWebUsername }
        set(value) { loginStore.putLoginData("username", value); mWebUsername = value }
    private var mWebPassword: String? = null
    var webPassword: String?
        get() { mWebPassword = mWebPassword ?: loginStore.getLoginData("password", null); return mWebPassword }
        set(value) { loginStore.putLoginData("password", value); mWebPassword = value }

    private var mWebSessionId: String? = null
    var webSessionId: String?
        get() { mWebSessionId = mWebSessionId ?: loginStore.getLoginData("webSessionId", null); return mWebSessionId }
        set(value) { loginStore.putLoginData("webSessionId", value); mWebSessionId = value }
    private var mWebAuth: String? = null
    var webAuth: String?
        get() { mWebAuth = mWebAuth ?: loginStore.getLoginData("webAuth", null); return mWebAuth }
        set(value) { loginStore.putLoginData("webAuth", value); mWebAuth = value }

    /*                   _
             /\         (_)
            /  \   _ __  _
           / /\ \ | '_ \| |
          / ____ \| |_) | |
         /_/    \_\ .__/|_|
                  | |
                  |*/
    private var mApiBearer: String? = null
    var apiBearer: String?
        get() { mApiBearer = mApiBearer ?: loginStore.getLoginData("apiBearer", null); return mApiBearer }
        set(value) { loginStore.putLoginData("apiBearer", value); mApiBearer = value }

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

    private var mRegisterId: Int? = null
    var registerId: Int
        get() { mRegisterId = mRegisterId ?: profile?.getStudentData("registerId", 0); return mRegisterId ?: 0 }
        set(value) { profile?.putStudentData("registerId", value) ?: return; mRegisterId = value }

    private var mSchoolYearId: Int? = null
    var schoolYearId: Int
        get() { mSchoolYearId = mSchoolYearId ?: profile?.getStudentData("schoolYearId", 0); return mSchoolYearId ?: 0 }
        set(value) { profile?.putStudentData("schoolYearId", value) ?: return; mSchoolYearId = value }
}
