/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.*
import pl.szczodrzynski.edziennik.data.api.task.IApiTask
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.ext.getString

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
    init { run {
        val type = message.data.getString("objectType") ?: return@run
        val accountLogin = message.data.getString("userId")?.replace("u", "") ?: return@run

        /* ./src/store/modules/helpers/change-processor.js */
        val endpoints = when (type) {
            "Notes" -> setOf(ENDPOINT_LIBRUS_API_NOTICES)
            "Grades" -> setOf(ENDPOINT_LIBRUS_API_NORMAL_GRADES, ENDPOINT_LIBRUS_API_NORMAL_GRADE_CATEGORIES, ENDPOINT_LIBRUS_API_NORMAL_GRADE_COMMENTS)
            "PointGrades" -> setOf(ENDPOINT_LIBRUS_API_POINT_GRADES, ENDPOINT_LIBRUS_API_POINT_GRADE_CATEGORIES)
            "DescriptiveGrades" -> setOf(ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES)
            "DescriptiveGrades/Text/Categories" -> setOf(ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADE_CATEGORIES)
            "DescriptiveTextGrades" -> setOf(ENDPOINT_LIBRUS_API_DESCRIPTIVE_TEXT_GRADES)
            "TextGrades" -> setOf(ENDPOINT_LIBRUS_API_TEXT_GRADES)
            "BehaviourGrades/Points" -> setOf(ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES, ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_CATEGORIES, ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_COMMENTS)
            "BehaviourGrades" -> setOf()
            "Attendances" -> setOf(ENDPOINT_LIBRUS_API_ATTENDANCES)
            "HomeWorks" -> setOf(ENDPOINT_LIBRUS_API_EVENTS)
            "ParentTeacherConferences" -> setOf(ENDPOINT_LIBRUS_API_PT_MEETINGS)
            "Calendars/ClassFreeDays" -> setOf(ENDPOINT_LIBRUS_API_CLASS_FREE_DAYS)
            "Calendars/TeacherFreeDays" -> setOf(ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS)
            "Calendars/SchoolFreeDays" -> setOf(ENDPOINT_LIBRUS_API_SCHOOL_FREE_DAYS)
            "Calendars/Substitutions" -> setOf(ENDPOINT_LIBRUS_API_TIMETABLES)
            "HomeWorkAssignments" -> setOf(ENDPOINT_LIBRUS_API_HOMEWORK, ENDPOINT_LIBRUS_SYNERGIA_HOMEWORK)
            "SchoolNotices" -> setOf(ENDPOINT_LIBRUS_API_ANNOUNCEMENTS)
            "Messages" -> setOf(ENDPOINT_LIBRUS_MESSAGES_RECEIVED)
            "LuckyNumbers" -> setOf(ENDPOINT_LIBRUS_API_LUCKY_NUMBER)
            "Timetables" -> setOf(ENDPOINT_LIBRUS_API_TIMETABLES)
            else -> return@run
        }

        if (endpoints.isEmpty())
            return@run

        val tasks = profiles.filter {
            it.loginStoreType == LoginType.LIBRUS &&
                    it.getStudentData("accountLogin", "")?.replace("u", "") == accountLogin
        }.map {
            EdziennikTask.syncProfile(it.id, setOf(FeatureType.ALWAYS_NEEDED), onlyEndpoints = endpoints)
        }
        IApiTask.enqueueAll(app, tasks)
    }}
}
