/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-23
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.synergia

import pl.szczodrzynski.edziennik.MONTH
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_SYNERGIA_INFO
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusSynergia

class LibrusSynergiaInfo(override val data: DataLibrus, val onSuccess: () -> Unit) : LibrusSynergia(data) {
    companion object {
        const val TAG = "LibrusSynergiaInfo"
    }

    init {
        synergiaGet(TAG, "informacja") { doc ->

            doc.select("table.form tbody").firstOrNull()?.children()?.also { info ->
                val studentNumber = info[2].select("td").text().trim().toIntOrNull()

                studentNumber?.also {
                    data.profile?.studentNumber = it
                }
            }

            data.setSyncNext(ENDPOINT_LIBRUS_SYNERGIA_INFO, MONTH)
            onSuccess()
        }
    }
}
