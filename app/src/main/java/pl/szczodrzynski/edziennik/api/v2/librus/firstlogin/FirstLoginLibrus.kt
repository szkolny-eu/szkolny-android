package pl.szczodrzynski.edziennik.api.v2.librus.firstlogin

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile

class FirstLoginLibrus(val app: App, val loginStore: LoginStore, val progressCallback: ProgressCallback, val onSuccess: (profileList: List<Profile>) -> Unit) {
    init {

    }
}