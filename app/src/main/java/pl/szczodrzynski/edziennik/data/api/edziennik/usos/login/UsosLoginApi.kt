/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.login

import pl.szczodrzynski.edziennik.data.api.ERROR_PROFILE_MISSING
import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_OAUTH_LOGIN_REQUEST
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.models.ApiError

class UsosLoginApi(val data: DataUsos, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "UsosLoginApi"
    }

    init { run {
        if (data.profile == null) {
            data.error(ApiError(TAG, ERROR_PROFILE_MISSING))
            return@run
        }

        if (data.isApiLoginValid()) {
            onSuccess()
        } else {
            data.error(ApiError(TAG, ERROR_USOS_OAUTH_LOGIN_REQUEST))
        }
    }}
}
