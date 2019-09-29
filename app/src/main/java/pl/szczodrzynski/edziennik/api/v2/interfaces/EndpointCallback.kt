/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.api.v2.interfaces

import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.api.v2.models.Endpoint
import pl.szczodrzynski.edziennik.api.v2.models.LoginMethod

/**
 * A callback passed to all [Endpoint]s and [LoginMethod]s
 */
interface EndpointCallback {
    fun onError(apiError: ApiError)
    fun onProgress(step: Int)
    fun onStartProgress(stringRes: Int)
}