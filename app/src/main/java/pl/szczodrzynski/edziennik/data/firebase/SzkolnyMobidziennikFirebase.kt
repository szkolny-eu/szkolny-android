/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_ATTENDANCE
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_BEHAVIOUR
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_GRADES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_MOBIDZIENNIK
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.task.IApiTask
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ext.getLong
import pl.szczodrzynski.edziennik.ext.getString

class SzkolnyMobidziennikFirebase(val app: App, val profiles: List<Profile>, val message: FirebaseService.Message) {
    /*{
      "id": "123456",
      "body": "Janósz Kowalski (Nauczyciele) - Temat wiadomości",
      "icon": "push",
      "type": "wiadOdebrana",
      "color": "#025b8e",
      "login": "1234@2019@szkola",
      "notId": "1234567",
      "sound": "default",
      "title": "Nowa wiadomość - mobiDziennik",
      "global_id": "123456",
      "vibrate": "true",
      "sync_url": "https://szkola.mobidziennik.pl/api2/logowanie"
    }*/
    /*{
      "body": "Kowalski Janósz - zapowiedziany sprawdzian na jutro:\njęzyk niemiecki (kartkówka - nieregularne 2)",
      "icon": "push",
      "type": "sprawdzianyJutro",
      "color": "#025b8e",
      "login": "1234@2019@szkola",
      "notId": "1234567",
      "sound": "default",
      "title": "Sprawdziany jutro - mobiDziennik",
      "global_id": "123456",
      "vibrate": "true",
      "sync_url": "https://szkola.mobidziennik.pl/api2/logowanie"
    }*/
    init { run {
        val type = message.data.getString("type") ?: return@run
        if (type == "sprawdzianyJutro" || type == "zadaniaJutro" || type == "autoryzacjaUrzadzenia")
            return@run
        val globalId = message.data.getLong("global_id")

        /* assets/www/js/push.js */
        val viewIdPair = when (type) {
            "wiadOdebrana" -> DRAWER_ITEM_MESSAGES to TYPE_RECEIVED
            "oceny", "ocenyKoncowe", "zachowanie" -> DRAWER_ITEM_GRADES to 0
            "uwagi" -> DRAWER_ITEM_BEHAVIOUR to 0
            "nieobecnoscPierwszaLekcja", "nieobecnosciDzisiaj" -> DRAWER_ITEM_ATTENDANCE to 0
            else -> return@run
        }

        val tasks = profiles.filter {
            it.loginStoreType == LOGIN_TYPE_MOBIDZIENNIK &&
                    it.getStudentData("globalId", 0L) == globalId
        }.map {
            EdziennikTask.syncProfile(it.id, listOf(viewIdPair))
        }
        IApiTask.enqueueAll(app, tasks)
    }}
}
