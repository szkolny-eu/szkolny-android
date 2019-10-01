/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-1.
 */

package pl.szczodrzynski.edziennik.api.v2.librus

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApiMe
import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginLibrusApi
import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginLibrusMessages
import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginLibrusPortal
import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginLibrusSynergia
import pl.szczodrzynski.edziennik.utils.Utils

class LibrusEndpoints(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LibrusEndpoints"
    }

    private var cancelled = false

    init {
        nextEndpoint(onSuccess)
    }

    private fun nextEndpoint(onSuccess: () -> Unit) {
        if (data.targetEndpointIds.isEmpty()) {
            onSuccess()
            return
        }
        useEndpoint(data.targetEndpointIds.removeAt(0)) {
            if (cancelled) {
                onSuccess()
                return@useEndpoint
            }
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, onSuccess: () -> Unit) {
        Utils.d(TAG, "Using endpoint $endpointId")
        when (endpointId) {
            ENDPOINT_LIBRUS_API_ME -> {
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
                LibrusApiMe(data) { onSuccess() }
            }
            else -> onSuccess()
        }
    }
}