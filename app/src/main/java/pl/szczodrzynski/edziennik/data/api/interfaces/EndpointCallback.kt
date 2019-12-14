/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.data.api.interfaces

import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.models.Feature
import pl.szczodrzynski.edziennik.data.api.models.LoginMethod

/**
 * A callback passed to all [Feature]s and [LoginMethod]s
 */
interface EndpointCallback {
    fun onError(apiError: ApiError)
    fun onProgress(step: Float)
    fun onStartProgress(stringRes: Int)
}
