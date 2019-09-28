package pl.szczodrzynski.edziennik.data.api.v2

import pl.szczodrzynski.edziennik.data.api.AppError
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

data class FirstLoginResult(val profileList: ArrayList<Profile>, val error: AppError?)
