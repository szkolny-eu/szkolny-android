/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.task.IApiTask
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.ext.toJsonObject

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
        val featureType = when (type.lowercase()) {
            "wiadomosc" -> FeatureType.MESSAGES_INBOX
            "ocena" -> FeatureType.GRADES
            "uwaga" -> FeatureType.BEHAVIOUR
            "frekwencja" -> FeatureType.ATTENDANCE
            // this type is not even implemented in Dzienniczek+
            "sprawdzian" -> FeatureType.AGENDA
            else -> return@run
        }

        val tasks = profiles.filter {
            it.loginStoreType == LoginType.VULCAN
                    && (it.getStudentData("studentId", 0) == studentId
                    || it.getStudentData("studentLoginId", 0) == loginId)
        }.map {
            EdziennikTask.syncProfile(it.id, setOf(featureType))
        }
        IApiTask.enqueueAll(app, tasks)
    }}
}
