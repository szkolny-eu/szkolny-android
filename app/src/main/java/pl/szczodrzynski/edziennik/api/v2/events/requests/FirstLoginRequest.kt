package pl.szczodrzynski.edziennik.api.v2.events.requests

import pl.szczodrzynski.edziennik.api.v2.models.ApiTask
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore

data class FirstLoginRequest(val loginStore: LoginStore) : ApiTask(-1) {
    override fun toString(): String {
        return "FirstLoginRequest(loginStore=$loginStore)"
    }
}