package pl.szczodrzynski.edziennik.data.api.v2.librus.login

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore

class LoginJst(val app: App, val loginStore: LoginStore, val callback: ProgressCallback, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "librus.LoginJst"
    }

}
