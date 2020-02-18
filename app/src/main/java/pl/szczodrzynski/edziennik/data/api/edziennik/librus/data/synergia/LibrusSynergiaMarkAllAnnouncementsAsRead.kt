/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-26
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia

import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusSynergia
import pl.szczodrzynski.edziennik.data.db.entity.Metadata

class LibrusSynergiaMarkAllAnnouncementsAsRead(override val data: DataLibrus,
                                               val onSuccess: () -> Unit
) : LibrusSynergia(data, null) {
    companion object {
        const val TAG = "LibrusSynergiaMarkAllAnnouncementsAsRead"
    }

    init {
        synergiaGet(TAG, "ogloszenia") {
            data.app.db.metadataDao().setAllSeen(profileId, Metadata.TYPE_ANNOUNCEMENT, true)
            onSuccess()
        }
    }
}
