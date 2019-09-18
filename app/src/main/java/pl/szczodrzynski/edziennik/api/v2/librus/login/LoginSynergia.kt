package pl.szczodrzynski.edziennik.api.v2.librus.login

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.datamodels.LoginStore

class LoginSynergia(val app: App, val loginStore: LoginStore, val callback: ProgressCallback, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "librus.LoginSynergia"
    }

}