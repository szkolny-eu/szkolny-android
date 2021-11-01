/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_EDUDZIENNIK
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_ACCOUNT_NAME_START
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_STUDENTS_START
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.login.EdudziennikLoginWeb
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ext.fixName
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.getShortName
import pl.szczodrzynski.edziennik.ext.set

class EdudziennikFirstLogin(val data: DataEdudziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "EdudziennikFirstLogin"
    }

    private val web = EdudziennikWeb(data, null)
    private val profileList = mutableListOf<Profile>()

    init {
        val loginStoreId = data.loginStore.id
        val loginStoreType = LOGIN_TYPE_EDUDZIENNIK
        var firstProfileId = loginStoreId

        EdudziennikLoginWeb(data) {
            web.webGet(TAG, "") { text ->
                val accountNameLong = EDUDZIENNIK_ACCOUNT_NAME_START.find(text)?.get(1)?.fixName()

                EDUDZIENNIK_STUDENTS_START.findAll(text).forEach {
                    val studentId = it[1]
                    val studentNameLong = it[2].fixName()

                    if (studentId.isBlank() || studentNameLong.isBlank()) return@forEach

                    val studentNameShort = studentNameLong.getShortName()
                    val accountName = if (accountNameLong == studentNameLong) null else accountNameLong

                    val profile = Profile(
                            firstProfileId++,
                            loginStoreId,
                            loginStoreType,
                            studentNameLong,
                            data.loginEmail,
                            studentNameLong,
                            studentNameShort,
                            accountName
                    ).apply {
                        studentData["studentId"] = studentId
                    }
                    profileList.add(profile)
                }

                EventBus.getDefault().postSticky(FirstLoginFinishedEvent(profileList, data.loginStore))
                onSuccess()
            }
        }
    }
}
