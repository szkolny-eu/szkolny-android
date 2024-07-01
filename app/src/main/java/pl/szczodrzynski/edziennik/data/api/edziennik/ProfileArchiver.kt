/*
 * Copyright (c) Kuba Szczodrzyński 2020-8-25.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik

import android.content.Intent
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.ext.Intent
import pl.szczodrzynski.edziennik.utils.models.Date
import timber.log.Timber

class ProfileArchiver(val app: App, val profile: Profile) {
    companion object {
        private const val TAG = "ProfileArchiver"
    }

    init {
        if (profile.archiveId == null)
            profile.archiveId = profile.id
        Timber.d("Processing ${profile.name}#${profile.id}, archiveId = ${profile.archiveId}")

        profile.archived = true
        app.db.profileDao().add(profile)
        //app.db.metadataDao().setAllSeen(profile.id, true)
        app.db.notificationDao().clear(profile.id)
        app.db.endpointTimerDao().clear(profile.id)
        Timber.d("Archived profile ${profile.id} saved")
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

        Timber.d("New profile ID for ${profile.name}: ${profile.id}")

        when (profile.loginStoreType) {
            LoginType.LIBRUS -> {
                profile.studentData.remove("isPremium")
                profile.studentData.remove("pushDeviceId")
                profile.studentData.remove("startPointsSemester1")
                profile.studentData.remove("startPointsSemester2")
                profile.studentData.remove("enablePointGrades")
                profile.studentData.remove("enableDescriptiveGrades")
            }
            LoginType.MOBIDZIENNIK -> {}
            LoginType.VULCAN -> {
                // DataVulcan.isApiLoginValid() returns false so it will update the semester
                profile.studentData.remove("currentSemesterEndDate")
                profile.studentData.remove("studentSemesterId")
                profile.studentData.remove("studentSemesterNumber")
                profile.studentData.remove("semester1Id")
                profile.studentData.remove("semester2Id")
                profile.studentData.remove("studentClassId")
            }
            LoginType.IDZIENNIK -> {
                profile.studentData.remove("schoolYearId")
            }
            LoginType.EDUDZIENNIK -> {}
            LoginType.PODLASIE -> {}
            LoginType.USOS -> {}
            LoginType.DEMO -> {}
            LoginType.TEMPLATE -> {}
        }

        Timber.d("Processed student data: ${profile.studentData}")

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
