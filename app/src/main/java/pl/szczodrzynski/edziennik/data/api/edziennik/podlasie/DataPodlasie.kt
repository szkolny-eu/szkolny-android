package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_TEMPLATE_API
import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.isNotNullNorEmpty

class DataPodlasie(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isApiLoginValid() = apiToken.isNotNullNorEmpty()

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isApiLoginValid())
            loginMethods += LOGIN_METHOD_TEMPLATE_API
    }

    override fun generateUserCode(): String {
        TODO("Not yet implemented")
    }

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
}
