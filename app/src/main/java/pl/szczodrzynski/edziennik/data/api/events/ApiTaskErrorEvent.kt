/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.data.api.events

import pl.szczodrzynski.edziennik.data.api.models.ApiError

class ApiTaskErrorEvent(val error: ApiError)
