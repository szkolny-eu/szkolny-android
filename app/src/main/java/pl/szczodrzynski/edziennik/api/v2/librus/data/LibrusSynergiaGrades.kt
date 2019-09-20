package pl.szczodrzynski.edziennik.api.v2.librus.data

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.api.v2.models.DataStore
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile

class LibrusSynergiaGrades(val app: App,
                      val profile: Profile,
                      val loginStore: LoginStore,
                      val dataStore: DataStore,
                      val callback: ProgressCallback,
                      val onSuccess: () -> Unit) {

    init {

    }
}