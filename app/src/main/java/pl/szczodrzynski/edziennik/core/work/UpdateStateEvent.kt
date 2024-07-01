/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-22.
 */

package pl.szczodrzynski.edziennik.core.work

import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update

class UpdateStateEvent(val running: Boolean, val update: Update?, val downloadId: Long)
