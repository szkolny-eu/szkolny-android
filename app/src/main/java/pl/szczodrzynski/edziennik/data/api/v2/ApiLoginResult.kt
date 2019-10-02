package pl.szczodrzynski.edziennik.data.api.v2

import pl.szczodrzynski.edziennik.data.api.AppError
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore

data class ApiLoginResult(val loginStore: LoginStore, val error: AppError?)
