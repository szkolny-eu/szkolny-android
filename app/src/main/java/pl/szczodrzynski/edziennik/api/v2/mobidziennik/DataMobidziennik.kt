/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_MOBIDZIENNIK_WEB
import pl.szczodrzynski.edziennik.api.v2.models.Data
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.isNotNullNorEmpty

class DataMobidziennik(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isWebLoginValid() = webSessionIdExpiryTime-30 > currentTimeUnix()
            && webSessionValue.isNotNullNorEmpty()
            && webSessionKey.isNotNullNorEmpty()
            && webServerId.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isWebLoginValid()) {
            loginMethods += LOGIN_METHOD_MOBIDZIENNIK_WEB
        }
    }

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
}