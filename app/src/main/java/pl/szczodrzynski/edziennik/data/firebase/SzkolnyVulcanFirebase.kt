/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_VULCAN
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.task.IApiTask
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import java.util.*

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
    init { run {
        val data = message.data.getString("data")?.toJsonObject() ?: return@run
        val type = data.getString("table") ?: return@run
        val studentId = data.getInt("pupilid")
        val loginId = data.getInt("loginid")

        /* pl.vulcan.uonetmobile.auxilary.enums.CDCPushEnum */
        val viewIdPair = when (type.toLowerCase(Locale.ROOT)) {
            "wiadomosc" -> MainActivity.DRAWER_ITEM_MESSAGES to Message.TYPE_RECEIVED
            "ocena" -> MainActivity.DRAWER_ITEM_GRADES to 0
            "uwaga" -> MainActivity.DRAWER_ITEM_BEHAVIOUR to 0
            "frekwencja" -> MainActivity.DRAWER_ITEM_ATTENDANCE to 0
            // this type is not even implemented in Dzienniczek+
            "sprawdzian" -> MainActivity.DRAWER_ITEM_AGENDA to 0
            else -> return@run
        }

        val tasks = profiles.filter {
            it.loginStoreType == LOGIN_TYPE_VULCAN
                    && (it.getStudentData("studentId", 0) == studentId
                    || it.getStudentData("studentLoginId", 0) == loginId)
        }.map {
            EdziennikTask.syncProfile(it.id, listOf(viewIdPair))
        }
        IApiTask.enqueueAll(app, tasks)
    }}
}
