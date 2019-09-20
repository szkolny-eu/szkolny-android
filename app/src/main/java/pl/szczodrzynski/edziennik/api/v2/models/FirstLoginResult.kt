package pl.szczodrzynski.edziennik.api.v2.models

import pl.szczodrzynski.edziennik.api.AppError
import pl.szczodrzynski.edziennik.datamodels.Profile

data class FirstLoginResult(val profileList: ArrayList<Profile>, val error: AppError?)