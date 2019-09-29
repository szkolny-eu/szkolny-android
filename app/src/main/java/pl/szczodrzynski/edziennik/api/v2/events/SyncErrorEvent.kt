/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.api.v2.events

import pl.szczodrzynski.edziennik.api.v2.models.ApiError

class SyncErrorEvent(val error: ApiError)