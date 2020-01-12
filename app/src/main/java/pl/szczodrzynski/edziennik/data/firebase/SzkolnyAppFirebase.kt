/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Profile

class SzkolnyAppFirebase(val app: App, val profiles: List<Profile>, val message: FirebaseService.Message) {
    init {

    }
}
