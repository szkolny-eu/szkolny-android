package pl.szczodrzynski.edziennik.data.api.events

import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

data class FirstLoginFinishedEvent(val profileList: List<Profile>, val loginStore: LoginStore)
