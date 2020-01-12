/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Profile

class SzkolnyLibrusFirebase(val app: App, val profiles: List<Profile>, val message: FirebaseService.Message) {
    /*{
      "gcm.notification.e": "1",
      "userId": "1234567u",
      "gcm.notification.sound": "default",
      "gcm.notification.title": "Synergia",
      "gcm.notification.sound2": "notify",
      "image": "www/assets/images/iconPush_01.png",
      "gcm.notification.body": "Dodano nieobecność nauczyciela od godziny 15:30 do godziny 16:15",
      "gcm.notification.icon": "notification_event.png",
      "objectType": "Calendars/TeacherFreeDays",
    }*/
    init {

    }
}
