/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-4.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS

class LibrusApiTemplate(override val data: DataLibrus,
                        val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApi"
    }

    init {
        /*apiGet(TAG, "") { json ->

            data.setSyncNext(ENDPOINT_LIBRUS_API_, SYNC_ALWAYS)
            onSuccess()
        }*/
    }
}
