/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-27. 
 */

package pl.szczodrzynski.edziennik.api.v2.idziennik.login

import pl.szczodrzynski.edziennik.api.v2.idziennik.DataIdziennik

class IdziennikLoginApi(val data: DataIdziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "IdziennikLoginApi"
    }

    init { run {
        if (data.isApiLoginValid()) {
            onSuccess()
        }
        else {
            onSuccess()
        }
    }}
}
