/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_VULCAN_HEBE
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_VULCAN_WEB_MAIN
import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ext.crc16
import pl.szczodrzynski.edziennik.ext.currentTimeUnix
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.fslogin.realm.RealmData

class DataVulcan(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {
    fun isWebMainLoginValid() = symbol.isNotNullNorEmpty()
            && (webExpiryTime[symbol]?.toLongOrNull() ?: 0) - 30 > currentTimeUnix()
            && webAuthCookie[symbol].isNotNullNorEmpty()
            && webRealmData != null
    fun isHebeLoginValid() = hebePublicKey.isNotNullNorEmpty()
            && hebePrivateKey.isNotNullNorEmpty()
            && symbol.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isWebMainLoginValid()) {
            loginMethods += LOGIN_METHOD_VULCAN_WEB_MAIN
        }
        if (isHebeLoginValid()) {
            loginMethods += LOGIN_METHOD_VULCAN_HEBE
        }
    }

    override fun generateUserCode() = "$schoolCode:$studentId"

    fun buildDeviceId(): String {
        val deviceId = app.deviceId.padStart(16, '0')
        val loginStoreId = loginStore.id.toString(16).padStart(4, '0')
        val symbol = symbol?.crc16()?.toString(16)?.take(2) ?: "00"
        return deviceId.substring(0..7) +
                "-" + deviceId.substring(8..11) +
                "-" + deviceId.substring(12..15) +
                "-" + loginStoreId +
                "-" + symbol + "6f72616e7a"
    }

    /**
     * A UONET+ client symbol.
     *
     * Present in the URL: https://uonetplus-uczen.vulcan.net.pl/[symbol]/[schoolSymbol]/
     *
     * e.g. "poznan"
     */
    private var mSymbol: String? = null
    var symbol: String?
        get() { mSymbol = mSymbol ?: profile?.getStudentData("symbol", null); return mSymbol }
        set(value) { profile?.putStudentData("symbol", value); mSymbol = value }

    /**
     * Group symbol/number of the student's school.
     *
     * Present in the URL: https://uonetplus-uczen.vulcan.net.pl/[symbol]/[schoolSymbol]/
     *
     * ListaUczniow/JednostkaSprawozdawczaSymbol, e.g. "000088"
     */
    private var mSchoolSymbol: String? = null
    var schoolSymbol: String?
        get() { mSchoolSymbol = mSchoolSymbol ?: profile?.getStudentData("schoolSymbol", null); return mSchoolSymbol }
        set(value) { profile?.putStudentData("schoolSymbol", value) ?: return; mSchoolSymbol = value }

    /**
     * Short name of the school, used in some places.
     *
     * ListaUczniow/JednostkaSprawozdawczaSkrot, e.g. "SP Wilkow"
     */
    private var mSchoolShort: String? = null
    var schoolShort: String?
        get() { mSchoolShort = mSchoolShort ?: profile?.getStudentData("schoolShort", null); return mSchoolShort }
        set(value) { profile?.putStudentData("schoolShort", value) ?: return; mSchoolShort = value }

    /**
     * A school code consisting of the [symbol] and [schoolSymbol].
     *
     * [symbol]_[schoolSymbol]
     *
     * e.g. "poznan_000088"
     */
    private var mSchoolCode: String? = null
    var schoolCode: String?
        get() { mSchoolCode = mSchoolCode ?: profile?.getStudentData("schoolName", null); return mSchoolCode }
        set(value) { profile?.putStudentData("schoolName", value) ?: return; mSchoolCode = value }

    /**
     * ID of the student.
     *
     * ListaUczniow/Id, e.g. 42632
     */
    private var mStudentId: Int? = null
    var studentId: Int
        get() { mStudentId = mStudentId ?: profile?.getStudentData("studentId", 0); return mStudentId ?: 0 }
        set(value) { profile?.putStudentData("studentId", value) ?: return; mStudentId = value }

    /**
     * ID of the student's account.
     *
     * ListaUczniow/UzytkownikLoginId, e.g. 1709
     */
    private var mStudentLoginId: Int? = null
    var studentLoginId: Int
        get() { mStudentLoginId = mStudentLoginId ?: profile?.getStudentData("studentLoginId", 0); return mStudentLoginId ?: 0 }
        set(value) { profile?.putStudentData("studentLoginId", value) ?: return; mStudentLoginId = value }

    /**
     * ID of the student's class.
     *
     * ListaUczniow/IdOddzial, e.g. 35
     */
    private var mStudentClassId: Int? = null
    var studentClassId: Int
        get() { mStudentClassId = mStudentClassId ?: profile?.getStudentData("studentClassId", 0); return mStudentClassId ?: 0 }
        set(value) { profile?.putStudentData("studentClassId", value) ?: return; mStudentClassId = value }

    /**
     * ListaUczniow/IdOkresKlasyfikacyjny, e.g. 321
     */
    private var mStudentSemesterId: Int? = null
    var studentSemesterId: Int
        get() { mStudentSemesterId = mStudentSemesterId ?: profile?.getStudentData("studentSemesterId", 0); return mStudentSemesterId ?: 0 }
        set(value) { profile?.putStudentData("studentSemesterId", value) ?: return; mStudentSemesterId = value }

    private var mStudentUnitId: Int? = null
    var studentUnitId: Int
        get() { mStudentUnitId = mStudentUnitId ?: profile?.getStudentData("studentUnitId", 0); return mStudentUnitId ?: 0 }
        set(value) { profile?.putStudentData("studentUnitId", value) ?: return; mStudentUnitId = value }

    private var mStudentConstituentId: Int? = null
    var studentConstituentId: Int
        get() { mStudentConstituentId = mStudentConstituentId ?: profile?.getStudentData("studentConstituentId", 0); return mStudentConstituentId ?: 0 }
        set(value) { profile?.putStudentData("studentConstituentId", value) ?: return; mStudentConstituentId = value }

    private var mSemester1Id: Int? = null
    var semester1Id: Int
        get() { mSemester1Id = mSemester1Id ?: profile?.getStudentData("semester1Id", 0); return mSemester1Id ?: 0 }
        set(value) { profile?.putStudentData("semester1Id", value) ?: return; mSemester1Id = value }
    private var mSemester2Id: Int? = null
    var semester2Id: Int
        get() { mSemester2Id = mSemester2Id ?: profile?.getStudentData("semester2Id", 0); return mSemester2Id ?: 0 }
        set(value) { profile?.putStudentData("semester2Id", value) ?: return; mSemester2Id = value }

    /**
     * ListaUczniow/OkresNumer, e.g. 1 or 2
     */
    private var mStudentSemesterNumber: Int? = null
    var studentSemesterNumber: Int
        get() { mStudentSemesterNumber = mStudentSemesterNumber ?: profile?.getStudentData("studentSemesterNumber", 0); return mStudentSemesterNumber ?: 0 }
        set(value) { profile?.putStudentData("studentSemesterNumber", value) ?: return; mStudentSemesterNumber = value }

    /**
     * Date of the end of the current semester ([studentSemesterNumber]).
     *
     * After this date, an API refresh of student list is required.
     */
    private var mCurrentSemesterEndDate: Long? = null
    var currentSemesterEndDate: Long
        get() { mCurrentSemesterEndDate = mCurrentSemesterEndDate ?: profile?.getStudentData("currentSemesterEndDate", 0L); return mCurrentSemesterEndDate ?: 0L }
        set(value) { profile?.putStudentData("currentSemesterEndDate", value) ?: return; mCurrentSemesterEndDate = value }

    /*             _____ _____        ____
             /\   |  __ \_   _|      |___ \
            /  \  | |__) || |   __   ____) |
           / /\ \ |  ___/ | |   \ \ / /__ <
          / ____ \| |    _| |_   \ V /___) |
         /_/    \_\_|   |_____|   \_/|___*/
    /**
     * A mobile API registration token.
     *
     * After first login only 3 first characters are stored here.
     * This is later used to determine the API URL address.
     */
    private var mApiToken: Map<String?, String?>? = null
    var apiToken: Map<String?, String?> = mapOf()
        get() { mApiToken = mApiToken ?: loginStore.getLoginData("apiToken", null)?.let { app.gson.fromJson(it, field.toMutableMap()::class.java) }; return mApiToken ?: mapOf() }
        set(value) { loginStore.putLoginData("apiToken", app.gson.toJson(value)); mApiToken = value }

    /**
     * A mobile API registration PIN.
     *
     * After first login, this is removed and/or set to null.
     */
    private var mApiPin: Map<String?, String?>? = null
    var apiPin: Map<String?, String?> = mapOf()
        get() { mApiPin = mApiPin ?: loginStore.getLoginData("apiPin", null)?.let { app.gson.fromJson(it, field.toMutableMap()::class.java) }; return mApiPin ?: mapOf() }
        set(value) { loginStore.putLoginData("apiPin", app.gson.toJson(value)); mApiPin = value }

    /*    _    _      _                     _____ _____ 
         | |  | |    | |              /\   |  __ \_   _|
         | |__| | ___| |__   ___     /  \  | |__) || |  
         |  __  |/ _ \ '_ \ / _ \   / /\ \ |  ___/ | |  
         | |  | |  __/ |_) |  __/  / ____ \| |    _| |_ 
         |_|  |_|\___|_.__/ \___| /_/    \_\_|   |____*/
    private var mHebePublicKey: String? = null
    var hebePublicKey: String?
        get() { mHebePublicKey = mHebePublicKey ?: loginStore.getLoginData("hebePublicKey", null); return mHebePublicKey }
        set(value) { loginStore.putLoginData("hebePublicKey", value); mHebePublicKey = value }

    private var mHebePrivateKey: String? = null
    var hebePrivateKey: String?
        get() { mHebePrivateKey = mHebePrivateKey ?: loginStore.getLoginData("hebePrivateKey", null); return mHebePrivateKey }
        set(value) { loginStore.putLoginData("hebePrivateKey", value); mHebePrivateKey = value }

    private var mHebePublicHash: String? = null
    var hebePublicHash: String?
        get() { mHebePublicHash = mHebePublicHash ?: loginStore.getLoginData("hebePublicHash", null); return mHebePublicHash }
        set(value) { loginStore.putLoginData("hebePublicHash", value); mHebePublicHash = value }

    private var mHebeContext: String? = null
    var hebeContext: String?
        get() { mHebeContext = mHebeContext ?: profile?.getStudentData("hebeContext", null); return mHebeContext }
        set(value) { profile?.putStudentData("hebeContext", value) ?: return; mHebeContext = value }

    private var mSenderAddressHash: String? = null
    var senderAddressHash: String?
        get() { mSenderAddressHash = mSenderAddressHash ?: profile?.getStudentData("senderAddressHash", null); return mSenderAddressHash }
        set(value) { profile?.putStudentData("senderAddressHash", value) ?: return; mSenderAddressHash = value }

    private var mSenderAddressName: String? = null
    var senderAddressName: String?
        get() { mSenderAddressName = mSenderAddressName ?: profile?.getStudentData("senderAddressName", null); return mSenderAddressName }
        set(value) { profile?.putStudentData("senderAddressName", value) ?: return; mSenderAddressName = value }

    val apiUrl: String?
        get() {
            val url = when (apiToken[symbol]?.substring(0, 3)) {
                "3S1" -> "https://lekcjaplus.vulcan.net.pl"
                "TA1" -> "https://uonetplus-komunikacja.umt.tarnow.pl"
                "OP1" -> "https://uonetplus-komunikacja.eszkola.opolskie.pl"
                "RZ1" -> "https://uonetplus-komunikacja.resman.pl"
                "GD1" -> "https://uonetplus-komunikacja.edu.gdansk.pl"
                "KA1" -> "https://uonetplus-komunikacja.mcuw.katowice.eu"
                "KA2" -> "https://uonetplus-komunikacja-test.mcuw.katowice.eu"
                "LU1" -> "https://uonetplus-komunikacja.edu.lublin.eu"
                "LU2" -> "https://test-uonetplus-komunikacja.edu.lublin.eu"
                "P03" -> "https://efeb-komunikacja-pro-efebmobile.pro.vulcan.pl"
                "P01" -> "http://efeb-komunikacja.pro-hudson.win.vulcan.pl"
                "P02" -> "http://efeb-komunikacja.pro-hudsonrc.win.vulcan.pl"
                "P90" -> "http://efeb-komunikacja-pro-mwujakowska.neo.win.vulcan.pl"
                "KO1" -> "https://uonetplus-komunikacja.eduportal.koszalin.pl"
                "FK1", "FS1" -> "http://api.fakelog.cf"
                "SZ9" -> "http://hack.szkolny.eu"
                else -> null
            }
            return if (url != null) "$url/$symbol/" else loginStore.getLoginData("apiUrl", null)
        }

    val fullApiUrl: String
        get() {
            return "$apiUrl$schoolSymbol/"
        }

    /*   __          __  _       ______ _____   _                 _
         \ \        / / | |     |  ____/ ____| | |               (_)
          \ \  /\  / /__| |__   | |__ | (___   | |     ___   __ _ _ _ __
           \ \/  \/ / _ \ '_ \  |  __| \___ \  | |    / _ \ / _` | | '_ \
            \  /\  /  __/ |_) | | |    ____) | | |___| (_) | (_| | | | | |
             \/  \/ \___|_.__/  |_|   |_____/  |______\___/ \__, |_|_| |_|
                                                             __/ |
                                                            |__*/
    var webRealmData: RealmData?
        get() { mWebRealmData = mWebRealmData ?: loginStore.getLoginData("webRealmData", JsonObject()).let {
                app.gson.fromJson(it, RealmData::class.java)
            }; return mWebRealmData }
        set(value) { loginStore.putLoginData("webRealmData", app.gson.toJsonTree(value) as JsonObject); mWebRealmData = value }
    private var mWebRealmData: RealmData? = null

    val webHost
        get() = webRealmData?.host

    var webEmail: String?
        get() { mWebEmail = mWebEmail ?: loginStore.getLoginData("webEmail", null); return mWebEmail }
        set(value) { loginStore.putLoginData("webEmail", value); mWebEmail = value }
    private var mWebEmail: String? = null
    var webUsername: String?
        get() { mWebUsername = mWebUsername ?: loginStore.getLoginData("webUsername", null); return mWebUsername }
        set(value) { loginStore.putLoginData("webUsername", value); mWebUsername = value }
    private var mWebUsername: String? = null
    var webPassword: String?
        get() { mWebPassword = mWebPassword ?: loginStore.getLoginData("webPassword", null); return mWebPassword }
        set(value) { loginStore.putLoginData("webPassword", value); mWebPassword = value }
    private var mWebPassword: String? = null

    /**
     * Expiry time of a certificate POSTed to a LoginEndpoint of the specific symbol.
     * If the time passes, the certificate needs to be POSTed again (if valid)
     * or re-generated.
     */
    var webExpiryTime: Map<String, String?> = mapOf()
        get() { mWebExpiryTime = mWebExpiryTime ?: loginStore.getLoginData("webExpiryTime", null)?.let { app.gson.fromJson(it, field.toMutableMap()::class.java) }; return mWebExpiryTime ?: mapOf() }
        set(value) { loginStore.putLoginData("webExpiryTime", app.gson.toJson(value)); mWebExpiryTime = value }
    private var mWebExpiryTime: Map<String, String?>? = null

    /**
     * EfebSsoAuthCookie retrieved after posting a certificate
     */
    var webAuthCookie: Map<String, String?> = mapOf()
        get() { mWebAuthCookie = mWebAuthCookie ?: loginStore.getLoginData("webAuthCookie", null)?.let { app.gson.fromJson(it, field.toMutableMap()::class.java) }; return mWebAuthCookie ?: mapOf() }
        set(value) { loginStore.putLoginData("webAuthCookie", app.gson.toJson(value)); mWebAuthCookie = value }
    private var mWebAuthCookie: Map<String, String?>? = null

    /**
     * Permissions needed to get JSONs from home page
     */
    var webPermissions: Map<String, String?> = mapOf()
        get() { mWebPermissions = mWebPermissions ?: loginStore.getLoginData("webPermissions", null)?.let { app.gson.fromJson(it, field.toMutableMap()::class.java) }; return mWebPermissions ?: mapOf() }
        set(value) { loginStore.putLoginData("webPermissions", app.gson.toJson(value)); mWebPermissions = value }
    private var mWebPermissions: Map<String, String?>? = null
}
