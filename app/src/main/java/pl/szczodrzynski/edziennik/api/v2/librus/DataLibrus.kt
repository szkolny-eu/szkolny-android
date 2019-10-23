/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2.librus

import okhttp3.Cookie
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_LIBRUS_API
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_LIBRUS_MESSAGES
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_LIBRUS_PORTAL
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_LIBRUS_SYNERGIA
import pl.szczodrzynski.edziennik.api.v2.models.Data
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.isNotNullNorEmpty

class DataLibrus(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isPortalLoginValid() = portalTokenExpiryTime-30 > currentTimeUnix() && portalRefreshToken.isNotNullNorEmpty() && portalAccessToken.isNotNullNorEmpty()
    fun isApiLoginValid() = apiTokenExpiryTime-30 > currentTimeUnix() && apiAccessToken.isNotNullNorEmpty()
    fun isSynergiaLoginValid() = synergiaSessionIdExpiryTime-30 > currentTimeUnix() && synergiaSessionId.isNotNullNorEmpty()
    fun isMessagesLoginValid() = messagesSessionIdExpiryTime-30 > currentTimeUnix() && messagesSessionId.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isPortalLoginValid())
            loginMethods += LOGIN_METHOD_LIBRUS_PORTAL
        if (isApiLoginValid())
            loginMethods += LOGIN_METHOD_LIBRUS_API
        if (isSynergiaLoginValid()) {
            loginMethods += LOGIN_METHOD_LIBRUS_SYNERGIA
            app.cookieJar.saveFromResponse(null, listOf(
                    Cookie.Builder()
                            .name("DZIENNIKSID")
                            .value(synergiaSessionId!!)
                            .domain("synergia.librus.pl")
                            .secure().httpOnly().build()
            ))
        }
        if (isMessagesLoginValid()) {
            loginMethods += LOGIN_METHOD_LIBRUS_MESSAGES
            app.cookieJar.saveFromResponse(null, listOf(
                    Cookie.Builder()
                            .name("DZIENNIKSID")
                            .value(messagesSessionId!!)
                            .domain("wiadomosci.librus.pl")
                            .secure().httpOnly().build()
            ))
        }
    }

    /*    _____           _        _
         |  __ \         | |      | |
         | |__) |__  _ __| |_ __ _| |
         |  ___/ _ \| '__| __/ _` | |
         | |  | (_) | |  | || (_| | |
         |_|   \___/|_|   \__\__,_|*/
    private var mPortalEmail: String? = null
    var portalEmail: String?
        get() { mPortalEmail = mPortalEmail ?: loginStore.getLoginData("email", null); return mPortalEmail }
        set(value) { loginStore.putLoginData("email", value); mPortalEmail = value }
    private var mPortalPassword: String? = null
    var portalPassword: String?
        get() { mPortalPassword = mPortalPassword ?: loginStore.getLoginData("password", null); return mPortalPassword }
        set(value) { loginStore.putLoginData("password", value); mPortalPassword = value }

    private var mPortalAccessToken: String? = null
    var portalAccessToken: String?
        get() { mPortalAccessToken = mPortalAccessToken ?: loginStore.getLoginData("accessToken", null); return mPortalAccessToken }
        set(value) { loginStore.putLoginData("accessToken", value); mPortalAccessToken = value }
    private var mPortalRefreshToken: String? = null
    var portalRefreshToken: String?
        get() { mPortalRefreshToken = mPortalRefreshToken ?: loginStore.getLoginData("refreshToken", null); return mPortalRefreshToken }
        set(value) { loginStore.putLoginData("refreshToken", value); mPortalRefreshToken = value }
    private var mPortalTokenExpiryTime: Long? = null
    var portalTokenExpiryTime: Long
        get() { mPortalTokenExpiryTime = mPortalTokenExpiryTime ?: loginStore.getLoginData("tokenExpiryTime", 0L); return mPortalTokenExpiryTime ?: 0L }
        set(value) { loginStore.putLoginData("tokenExpiryTime", value); mPortalTokenExpiryTime = value }

    /*             _____ _____
             /\   |  __ \_   _|
            /  \  | |__) || |
           / /\ \ |  ___/ | |
          / ____ \| |    _| |_
         /_/    \_\_|   |____*/
    /**
     * A Synergia login, like 1234567u.
     * Used: for login (API Login Method) in Synergia mode.
     * Used: for login (Synergia Login Method) in Synergia mode.
     * And also in various places in [pl.szczodrzynski.edziennik.api.v2.models.Feature]s
     */
    private var mApiLogin: String? = null
    var apiLogin: String?
        get() { mApiLogin = mApiLogin ?: profile?.getStudentData("accountLogin", null); return mApiLogin }
        set(value) { profile?.putStudentData("accountLogin", value) ?: return; mApiLogin = value }
    /**
     * A Synergia password.
     * Used: for login (API Login Method) in Synergia mode.
     * Used: for login (Synergia Login Method) in Synergia mode.
     */
    private var mApiPassword: String? = null
    var apiPassword: String?
        get() { mApiPassword = mApiPassword ?: profile?.getStudentData("accountPassword", null); return mApiPassword }
        set(value) { profile?.putStudentData("accountPassword", value) ?: return; mApiPassword = value }

    /**
     * A JST login Code.
     * Used only during first login in JST mode.
     */
    private var mApiCode: String? = null
    var apiCode: String?
        get() { mApiCode = mApiCode ?: profile?.getStudentData("accountCode", null); return mApiCode }
        set(value) { profile?.putStudentData("accountCode", value) ?: return; mApiCode = value }
    /**
     * A JST login PIN.
     * Used only during first login in JST mode.
     */
    private var mApiPin: String? = null
    var apiPin: String?
        get() { mApiPin = mApiPin ?: profile?.getStudentData("accountPin", null); return mApiPin }
        set(value) { profile?.putStudentData("accountPin", value) ?: return; mApiPin = value }

    /**
     * A Synergia API access token.
     * Used in all Api Endpoints.
     * Created in Login Method Api.
     * Applicable for all login modes.
     */
    private var mApiAccessToken: String? = null
    var apiAccessToken: String?
        get() { mApiAccessToken = mApiAccessToken ?: profile?.getStudentData("accountToken", null); return mApiAccessToken }
        set(value) { profile?.putStudentData("accountToken", value) ?: return; mApiAccessToken = value }
    /**
     * A Synergia API refresh token.
     * Used when refreshing the [apiAccessToken] in JST, Synergia modes.
     */
    private var mApiRefreshToken: String? = null
    var apiRefreshToken: String?
        get() { mApiRefreshToken = mApiRefreshToken ?: profile?.getStudentData("accountRefreshToken", null); return mApiRefreshToken }
        set(value) { profile?.putStudentData("accountRefreshToken", value) ?: return; mApiRefreshToken = value }
    /**
     * The expiry time for [apiAccessToken], as a UNIX timestamp.
     * Used when refreshing the [apiAccessToken] in JST, Synergia modes.
     * Used when refreshing the [apiAccessToken] in Portal mode ([pl.szczodrzynski.edziennik.api.v2.librus.login.SynergiaTokenExtractor])
     */
    private var mApiTokenExpiryTime: Long? = null
    var apiTokenExpiryTime: Long
        get() { mApiTokenExpiryTime = mApiTokenExpiryTime ?: profile?.getStudentData("accountTokenTime", 0L); return mApiTokenExpiryTime ?: 0L }
        set(value) { profile?.putStudentData("accountTokenTime", value) ?: return; mApiTokenExpiryTime = value }

    /*     _____                            _
          / ____|                          (_)
         | (___  _   _ _ __   ___ _ __ __ _ _  __ _
          \___ \| | | | '_ \ / _ \ '__/ _` | |/ _` |
          ____) | |_| | | | |  __/ | | (_| | | (_| |
         |_____/ \__, |_| |_|\___|_|  \__, |_|\__,_|
                  __/ |                __/ |
                 |___/                |__*/
    /**
     * A Synergia web Session ID (DZIENNIKSID).
     * Used in endpoints with Synergia login method.
     */
    private var mSynergiaSessionId: String? = null
    var synergiaSessionId: String?
        get() { mSynergiaSessionId = mSynergiaSessionId ?: profile?.getStudentData("accountSID", null); return mSynergiaSessionId }
        set(value) { profile?.putStudentData("accountSID", value) ?: return; mSynergiaSessionId = value }
    /**
     * The expiry time for [synergiaSessionId], as a UNIX timestamp.
     * Used in endpoints with Synergia login method.
     * TODO verify how long is the session ID valid.
     */
    private var mSynergiaSessionIdExpiryTime: Long? = null
    var synergiaSessionIdExpiryTime: Long
        get() { mSynergiaSessionIdExpiryTime = mSynergiaSessionIdExpiryTime ?: profile?.getStudentData("accountSIDTime", 0L); return mSynergiaSessionIdExpiryTime ?: 0L }
        set(value) { profile?.putStudentData("accountSIDTime", value) ?: return; mSynergiaSessionIdExpiryTime = value }


    /**
     * A Messages web Session ID (DZIENNIKSID).
     * Used in endpoints with Messages login method.
     */
    private var mMessagesSessionId: String? = null
    var messagesSessionId: String?
        get() { mMessagesSessionId = mMessagesSessionId ?: profile?.getStudentData("messagesSID", null); return mMessagesSessionId }
        set(value) { profile?.putStudentData("messagesSID", value) ?: return; mMessagesSessionId = value }
    /**
     * The expiry time for [messagesSessionId], as a UNIX timestamp.
     * Used in endpoints with Messages login method.
     * TODO verify how long is the session ID valid.
     */
    private var mMessagesSessionIdExpiryTime: Long? = null
    var messagesSessionIdExpiryTime: Long
        get() { mMessagesSessionIdExpiryTime = mMessagesSessionIdExpiryTime ?: profile?.getStudentData("messagesSIDTime", 0L); return mMessagesSessionIdExpiryTime ?: 0L }
        set(value) { profile?.putStudentData("messagesSIDTime", value) ?: return; mMessagesSessionIdExpiryTime = value }

    /*     ____  _   _
          / __ \| | | |
         | |  | | |_| |__   ___ _ __
         | |  | | __| '_ \ / _ \ '__|
         | |__| | |_| | | |  __/ |
          \____/ \__|_| |_|\___|*/
    var isPremium
        get() = profile?.getStudentData("isPremium", false) ?: false
        set(value) { profile?.putStudentData("isPremium", value) }

    private var mSchoolName: String? = null
    var schoolName: String?
        get() { mSchoolName = mSchoolName ?: profile?.getStudentData("schoolName", null); return mSchoolName }
        set(value) { profile?.putStudentData("schoolName", value) ?: return; mSchoolName = value }

    private var mUnitId: Long? = null
    var unitId: Long
        get() { mUnitId = mUnitId ?: profile?.getStudentData("unitId", 0L); return mUnitId ?: 0L }
        set(value) { profile?.putStudentData("unitId", value) ?: return; mUnitId = value }

    private var mStartPointsSemester1: Int? = null
    var startPointsSemester1: Int
        get() { mStartPointsSemester1 = mStartPointsSemester1 ?: profile?.getStudentData("startPointsSemester1", 0); return mStartPointsSemester1 ?: 0 }
        set(value) { profile?.putStudentData("startPointsSemester1", value) ?: return; mStartPointsSemester1 = value }
    private var mStartPointsSemester2: Int? = null
    var startPointsSemester2: Int
        get() { mStartPointsSemester2 = mStartPointsSemester2 ?: profile?.getStudentData("startPointsSemester2", 0); return mStartPointsSemester2 ?: 0 }
        set(value) { profile?.putStudentData("startPointsSemester2", value) ?: return; mStartPointsSemester2 = value }

    private var mEnablePointGrades: Boolean? = null
    var enablePointGrades: Boolean
        get() { mEnablePointGrades = mEnablePointGrades ?: profile?.getStudentData("enablePointGrades", true); return mEnablePointGrades ?: true }
        set(value) { profile?.putStudentData("enablePointGrades", value) ?: return; mEnablePointGrades = value }
    private var mEnableDescriptiveGrades: Boolean? = null
    var enableDescriptiveGrades: Boolean
        get() { mEnableDescriptiveGrades = mEnableDescriptiveGrades ?: profile?.getStudentData("enableDescriptiveGrades", true); return mEnableDescriptiveGrades ?: true }
        set(value) { profile?.putStudentData("enableDescriptiveGrades", value) ?: return; mEnableDescriptiveGrades = value }
}