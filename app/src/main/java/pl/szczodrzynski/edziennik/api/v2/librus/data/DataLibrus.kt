package pl.szczodrzynski.edziennik.api.v2.librus.data

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile

class DataLibrus(val app: App, val profile: Profile, val loginStore: LoginStore, val callback: ProgressCallback, val onSuccess: () -> Unit) {

}