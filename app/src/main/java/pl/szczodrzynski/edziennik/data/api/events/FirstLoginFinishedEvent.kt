package pl.szczodrzynski.edziennik.data.api.events

import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile

data class FirstLoginFinishedEvent(val profileList: List<Profile>, val loginStore: LoginStore)
