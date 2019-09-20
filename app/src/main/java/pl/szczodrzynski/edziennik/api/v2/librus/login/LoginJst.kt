package pl.szczodrzynski.edziennik.api.v2.librus.login

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.api.v2.interfaces.ILoginMethod
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile

class LoginJst(
        app: App,
        profile: Profile?,
        loginStore: LoginStore,
        callback: ProgressCallback,
        onSuccess: () -> Unit
): ILoginMethod(app, profile, loginStore, callback, onSuccess) {
    companion object {
        private const val TAG = "librus.LoginJst"
    }

}