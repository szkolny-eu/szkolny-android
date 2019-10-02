package pl.szczodrzynski.edziennik.data.api.v2.librus.firstlogin

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

class FirstLoginSynergia(val app: App, val loginStore: LoginStore, val progressCallback: ProgressCallback, val onSuccess: (profileList: List<Profile>) -> Unit) {
    init {

    }
}
