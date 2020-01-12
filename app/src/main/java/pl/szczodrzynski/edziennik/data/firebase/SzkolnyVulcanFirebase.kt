/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Profile

class SzkolnyVulcanFirebase(val app: App, val profiles: List<Profile>, val message: FirebaseService.Message) {
    /*{
      "data": {
        "loginid": 12345,
        "pupilid": 1234,
        "unitid": 2,
        "event": "CDC",
        "day": "2019-09-09",
        "table": "Frekwencja"
      },
      "title": "Frekwencja",
      "message: "Uczeń Janósz otrzymał nieobecność na 7 lekcji"
    }*/
    init {

    }
}
