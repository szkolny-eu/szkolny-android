/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_VULCAN_API
import pl.szczodrzynski.edziennik.api.v2.models.Data
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.isNotNullNorEmpty

class DataVulcan(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isApiLoginValid() = apiCertificateExpiryTime-30 > currentTimeUnix()
            && apiCertificateKey.isNotNullNorEmpty()
            && apiCertificatePfx.isNotNullNorEmpty()
            && symbol.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isApiLoginValid()) {
            loginMethods += LOGIN_METHOD_VULCAN_API
        }
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
        get() { mSymbol = mSymbol ?: loginStore.getLoginData("deviceSymbol", null); return mSymbol }
        set(value) { loginStore.putLoginData("deviceSymbol", value); mSymbol = value }

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
     * A school ID consisting of the [symbol] and [schoolSymbol].
     *
     * [symbol]_[schoolSymbol]
     *
     * e.g. "poznan_000088"
     */
    private var mSchoolName: String? = null
    var schoolName: String?
        get() { mSchoolName = mSchoolName ?: profile?.getStudentData("schoolName", null); return mSchoolName }
        set(value) { profile?.putStudentData("schoolName", value) ?: return; mSchoolName = value }

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

    /**
     * ListaUczniow/OkresNumer, e.g. 1 or 2
     */
    private var mStudentSemesterNumber: Int? = null
    var studentSemesterNumber: Int
        get() { mStudentSemesterNumber = mStudentSemesterNumber ?: profile?.getStudentData("studentSemesterNumber", 0); return mStudentSemesterNumber ?: 0 }
        set(value) { profile?.putStudentData("studentSemesterNumber", value) ?: return; mStudentSemesterNumber = value }

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
    private var mApiToken: String? = null
    var apiToken: String?
        get() { mApiToken = mApiToken ?: loginStore.getLoginData("deviceToken", null); return mApiToken }
        set(value) { loginStore.putLoginData("deviceToken", value); mApiToken = value }

    /**
     * A mobile API registration PIN.
     *
     * After first login, this is removed and/or set to null.
     */
    private var mApiPin: String? = null
    var apiPin: String?
        get() { mApiPin = mApiPin ?: loginStore.getLoginData("devicePin", null); return mApiPin }
        set(value) { loginStore.putLoginData("devicePin", value); mApiPin = value }

    private var mApiCertificateKey: String? = null
    var apiCertificateKey: String?
        get() { mApiCertificateKey = mApiCertificateKey ?: loginStore.getLoginData("certificateKey", null); return mApiCertificateKey }
        set(value) { loginStore.putLoginData("certificateKey", value); mApiCertificateKey = value }

    private var mApiCertificatePfx: String? = null
    var apiCertificatePfx: String?
        get() { mApiCertificatePfx = mApiCertificatePfx ?: loginStore.getLoginData("certificatePfx", null); return mApiCertificatePfx }
        set(value) { loginStore.putLoginData("certificatePfx", value); mApiCertificatePfx = value }

    private var mApiCertificateExpiryTime: Int? = null
    var apiCertificateExpiryTime: Int
        get() { mApiCertificateExpiryTime = mApiCertificateExpiryTime ?: loginStore.getLoginData("certificateExpiryTime", 0); return mApiCertificateExpiryTime ?: 0 }
        set(value) { loginStore.putLoginData("certificateExpiryTime", value); mApiCertificateExpiryTime = value }

    val apiUrl: String?
        get() {
            return when (apiToken?.substring(0, 3)) {
                "3S1" -> "https://lekcjaplus.vulcan.net.pl/$symbol/"
                "TA1" -> "https://uonetplus-komunikacja.umt.tarnow.pl/$symbol/"
                "OP1" -> "https://uonetplus-komunikacja.eszkola.opolskie.pl/$symbol/"
                "RZ1" -> "https://uonetplus-komunikacja.resman.pl/$symbol/"
                "GD1" -> "https://uonetplus-komunikacja.edu.gdansk.pl/$symbol/"
                "KA1" -> "https://uonetplus-komunikacja.mcuw.katowice.eu/$symbol/"
                "KA2" -> "https://uonetplus-komunikacja-test.mcuw.katowice.eu/$symbol/"
                "P03" -> "https://efeb-komunikacja-pro-efebmobile.pro.vulcan.pl/$symbol/"
                "P01" -> "http://efeb-komunikacja.pro-hudson.win.vulcan.pl/$symbol/"
                "P02" -> "http://efeb-komunikacja.pro-hudsonrc.win.vulcan.pl/$symbol/"
                "P90" -> "http://efeb-komunikacja-pro-mwujakowska.neo.win.vulcan.pl/$symbol/"
                "FK1", "FS1" -> "http://api.fakelog.cf/$symbol/"
                "SZ9" -> "http://vulcan.szkolny.eu/$symbol/"
                else -> null
            }
        }

    val fullApiUrl: String?
        get() {
            return "${apiUrl}${schoolSymbol}/"
        }
}
