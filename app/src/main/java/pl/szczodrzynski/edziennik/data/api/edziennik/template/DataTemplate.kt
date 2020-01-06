/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.template

import okhttp3.Cookie
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_TEMPLATE_API
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_TEMPLATE_WEB
import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.isNotNullNorEmpty

/**
 * Use http://patorjk.com/software/taag/#p=display&f=Big for the ascii art
 *
 * Use https://codepen.io/kubasz/pen/RwwwbGN to easily generate the student data getters/setters
 */
class DataTemplate(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isWebLoginValid() = webExpiryTime-30 > currentTimeUnix() && webCookie.isNotNullNorEmpty()
    fun isApiLoginValid() = apiExpiryTime-30 > currentTimeUnix() && apiToken.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isWebLoginValid()) {
            loginMethods += LOGIN_METHOD_TEMPLATE_WEB
            app.cookieJar.saveFromResponse(null, listOf(
                    Cookie.Builder()
                            .name("AuthCookie")
                            .value(webCookie!!)
                            .domain("eregister.example.com")
                            .secure().httpOnly().build()
            ))
        }
        if (isApiLoginValid())
            loginMethods += LOGIN_METHOD_TEMPLATE_API
    }

    override fun generateUserCode() = "TEMPLATE:DO_NOT_USE"

    /*   __          __  _
         \ \        / / | |
          \ \  /\  / /__| |__
           \ \/  \/ / _ \ '_ \
            \  /\  /  __/ |_) |
             \/  \/ \___|_._*/
    private var mWebCookie: String? = null
    var webCookie: String?
        get() { mWebCookie = mWebCookie ?: profile?.getStudentData("webCookie", null); return mWebCookie }
        set(value) { profile?.putStudentData("webCookie", value) ?: return; mWebCookie = value }

    private var mWebExpiryTime: Long? = null
    var webExpiryTime: Long
        get() { mWebExpiryTime = mWebExpiryTime ?: profile?.getStudentData("webExpiryTime", 0L); return mWebExpiryTime ?: 0L }
        set(value) { profile?.putStudentData("webExpiryTime", value) ?: return; mWebExpiryTime = value }

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
        get() { mApiToken = mApiToken ?: profile?.getStudentData("apiToken", null); return mApiToken }
        set(value) { profile?.putStudentData("apiToken", value) ?: return; mApiToken = value }

    private var mApiExpiryTime: Long? = null
    var apiExpiryTime: Long
        get() { mApiExpiryTime = mApiExpiryTime ?: profile?.getStudentData("apiExpiryTime", 0L); return mApiExpiryTime ?: 0L }
        set(value) { profile?.putStudentData("apiExpiryTime", value) ?: return; mApiExpiryTime = value }
}
