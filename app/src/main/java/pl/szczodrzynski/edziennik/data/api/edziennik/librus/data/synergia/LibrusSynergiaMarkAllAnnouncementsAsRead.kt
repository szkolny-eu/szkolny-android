/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-26
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia

import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusSynergia

class LibrusSynergiaMarkAllAnnouncementsAsRead(override val data: DataLibrus, val onSuccess: () -> Unit) : LibrusSynergia(data) {
    companion object {
        const val TAG = "LibrusSynergiaMarkAllAnnouncementsAsRead"
    }

    init {
        synergiaGet(TAG, "ogloszenia") {
            onSuccess()
        }
    }
}
