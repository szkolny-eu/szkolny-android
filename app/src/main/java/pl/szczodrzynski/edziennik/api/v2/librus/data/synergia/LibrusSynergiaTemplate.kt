/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-23
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.synergia

import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusSynergia
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS

class LibrusSynergiaTemplate(override val data: DataLibrus, val onSuccess: () -> Unit) : LibrusSynergia(data) {
    companion object {
        const val TAG = "LibrusSynergia"
    }

    init {
        /* synergiaGet(TAG, "") { doc ->

            data.setSyncNext(ENDPOINT_LIBRUS_SYNERGIA_, SYNC_ALWAYS)
            onSuccess()
        } */
    }
}
