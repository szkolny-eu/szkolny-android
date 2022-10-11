/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_USOS_API
import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile

class DataUsos(
    app: App,
    profile: Profile?,
    loginStore: LoginStore,
) : Data(app, profile, loginStore) {

    fun isApiLoginValid() = oauthTokenKey != null && oauthTokenSecret != null

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isApiLoginValid()) {
            loginMethods += LOGIN_METHOD_USOS_API
        }
    }

    override fun generateUserCode() = "USOS:TEST"

    var oauthTokenKey: String?
        get() { mOauthTokenKey = mOauthTokenKey ?: loginStore.getLoginData("oauthTokenKey", null); return mOauthTokenKey }
        set(value) { loginStore.putLoginData("oauthTokenKey", value); mOauthTokenKey = value }
    private var mOauthTokenKey: String? = null

    var oauthTokenSecret: String?
        get() { mOauthTokenSecret = mOauthTokenSecret ?: loginStore.getLoginData("oauthTokenSecret", null); return mOauthTokenSecret }
        set(value) { loginStore.putLoginData("oauthTokenSecret", value); mOauthTokenSecret = value }
    private var mOauthTokenSecret: String? = null
}
