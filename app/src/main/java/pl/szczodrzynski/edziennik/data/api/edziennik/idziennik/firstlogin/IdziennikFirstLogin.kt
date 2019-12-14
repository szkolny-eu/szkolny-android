/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-27. 
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_IDZIENNIK_FIRST_NO_SCHOOL_YEAR
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_SETTINGS
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.login.IdziennikLoginWeb
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.swapFirstLastName

class IdziennikFirstLogin(val data: DataIdziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "IdziennikFirstLogin"
    }

    private val web = IdziennikWeb(data)
    private val profileList = mutableListOf<Profile>()

    init {
        IdziennikLoginWeb(data) {
            web.webGet(TAG, IDZIENNIK_WEB_SETTINGS) { text ->
                //val accounts = json.getJsonArray("accounts")

                val isParent = Regexes.IDZIENNIK_LOGIN_FIRST_IS_PARENT.find(text)?.get(1) != "0"
                val accountNameLong = if (isParent)
                    Regexes.IDZIENNIK_LOGIN_FIRST_ACCOUNT_NAME.find(text)?.get(1)?.swapFirstLastName()?.fixName()
                else
                    null

                var schoolYearName: String? = null
                val schoolYear = Regexes.IDZIENNIK_LOGIN_FIRST_SCHOOL_YEAR.find(text)?.let {
                    schoolYearName = it[2]
                    it[1].toIntOrNull()
                } ?: run {
                    data.error(ApiError(TAG, ERROR_LOGIN_IDZIENNIK_FIRST_NO_SCHOOL_YEAR)
                            .withApiResponse(text))
                    return@webGet
                }

                Regexes.IDZIENNIK_LOGIN_FIRST_STUDENT.findAll(text)
                        .toMutableList()
                        .reversed()
                        .forEach { match ->
                            val registerId = match[1].toIntOrNull() ?: return@forEach
                            val studentId = match[2]
                            val firstName = match[3]
                            val lastName = match[4]
                            val className = match[5] + " " + match[6]

                            val profile = Profile()
                            profile.studentNameLong = "$firstName $lastName".fixName()
                            profile.studentNameShort = "$firstName ${lastName[0]}.".fixName()
                            profile.accountNameLong = accountNameLong
                            profile.studentClassName = className
                            profile.studentSchoolYear = schoolYearName
                            profile.name = profile.studentNameLong
                            profile.subname = data.webUsername
                            profile.empty = true
                            profile.putStudentData("studentId", studentId)
                            profile.putStudentData("registerId", registerId)
                            profile.putStudentData("schoolYearId", schoolYear)
                            profileList.add(profile)
                        }

                EventBus.getDefault().post(FirstLoginFinishedEvent(profileList, data.loginStore))
                onSuccess()
            }
        }
    }
}
