/*
 * Copyright (c) Kuba Szczodrzyński 2020-8-25.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik

import android.content.Intent
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.Intent
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date

class ProfileArchiver(val app: App, val profile: Profile) {
    companion object {
        private const val TAG = "ProfileArchiver"
    }

    init {
        if (profile.archiveId == null)
            profile.archiveId = profile.id
        d(TAG, "Processing ${profile.name}#${profile.id}, archiveId = ${profile.archiveId}")

        profile.archived = true
        app.db.profileDao().add(profile)
        //app.db.metadataDao().setAllSeen(profile.id, true)
        app.db.notificationDao().clear(profile.id)
        app.db.endpointTimerDao().clear(profile.id)
        d(TAG, "Archived profile ${profile.id} saved")
        profile.archived = false

        // guess the nearest school year
        val today = Date.getToday()
        profile.studentSchoolYearStart = when {
            today.month <= profile.dateYearEnd.month -> today.year - 1
            else -> today.year
        }

        // set default semester dates
        profile.dateSemester1Start = Date(profile.studentSchoolYearStart, 9, 1)
        profile.dateSemester2Start = Date(profile.studentSchoolYearStart + 1, 2, 1)
        profile.dateYearEnd = Date(profile.studentSchoolYearStart + 1, 6, 30)

        val oldId = profile.id
        val newId = (app.db.profileDao().lastId ?: profile.id) + 1
        profile.id = newId
        profile.subname = "Nowy rok szkolny - ${profile.studentSchoolYearStart}"
        profile.studentClassName = null

        d(TAG, "New profile ID for ${profile.name}: ${profile.id}")

        when (profile.loginStoreType) {
            LOGIN_TYPE_LIBRUS -> {
                profile.removeStudentData("isPremium")
                profile.removeStudentData("pushDeviceId")
                profile.removeStudentData("startPointsSemester1")
                profile.removeStudentData("startPointsSemester2")
                profile.removeStudentData("enablePointGrades")
                profile.removeStudentData("enableDescriptiveGrades")
            }
            LOGIN_TYPE_MOBIDZIENNIK -> {

            }
            LOGIN_TYPE_VULCAN -> {
                // DataVulcan.isApiLoginValid() returns false so it will update the semester
                profile.removeStudentData("currentSemesterEndDate")
                profile.removeStudentData("studentSemesterId")
                profile.removeStudentData("studentSemesterNumber")
                profile.removeStudentData("semester1Id")
                profile.removeStudentData("semester2Id")
                profile.removeStudentData("studentClassId")
            }
            LOGIN_TYPE_IDZIENNIK -> {
                profile.removeStudentData("schoolYearId")
            }
            LOGIN_TYPE_EDUDZIENNIK -> {

            }
            LOGIN_TYPE_PODLASIE -> {

            }
        }

        d(TAG, "Processed student data: ${profile.studentData}")

        app.db.profileDao().add(profile)

        if (app.profileId == oldId) {
            val intent = Intent(
                    Intent.ACTION_MAIN,
                    "profileId" to newId
            )
            app.sendBroadcast(intent)
        }
    }
}
