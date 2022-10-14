/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-14.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.firstlogin

import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosApi
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.login.UsosLoginApi

class UsosFirstLogin(val data: DataUsos, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "UsosFirstLogin"
    }

    private val api = UsosApi(data, null)

    init {
        UsosLoginApi(data) {

        }
    }
}
