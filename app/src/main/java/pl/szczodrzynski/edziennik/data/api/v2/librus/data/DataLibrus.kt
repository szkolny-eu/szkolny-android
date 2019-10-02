package pl.szczodrzynski.edziennik.data.api.v2.librus.data

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

class DataLibrus(val app: App, val profile: Profile, val loginStore: LoginStore, val callback: ProgressCallback, val onSuccess: () -> Unit) {

}
