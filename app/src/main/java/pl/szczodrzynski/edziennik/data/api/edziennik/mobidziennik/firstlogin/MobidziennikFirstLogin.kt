package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.login.MobidziennikLoginWeb
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.utils.Utils

class MobidziennikFirstLogin(val data: DataMobidziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "MobidziennikFirstLogin"
    }

    private val web = MobidziennikWeb(data)
    private val profileList = mutableListOf<Profile>()

    init {
        MobidziennikLoginWeb(data) {
            web.webGet(TAG, "/api/zrzutbazy") { text ->
                val tables = text.split("T@B#LA")

                val accountNameLong = run {
                    tables[0]
                            .split("\n")
                            .map { it.split("|") }
                            .singleOrNull { it.getOrNull(1) != "*" }
                            ?.let {
                                "${it[4]} ${it[5]}".fixName()
                            }
                }

                tables[8].split("\n").forEach { student ->
                    if (student.isEmpty())
                        return@forEach
                    val student1 = student.split("|")
                    if (student1.size == 2)
                        return@forEach

                    val profile = Profile()
                    profile.studentNameLong = "${student1[2]} ${student1[4]}".fixName()
                    profile.studentNameShort = "${student1[2]} ${student1[4][0]}.".fixName()
                    profile.accountNameLong = if (accountNameLong == profile.studentNameLong) null else accountNameLong
                    profile.studentSchoolYear = Utils.getCurrentSchoolYear()
                    profile.name = profile.studentNameLong
                    profile.subname = data.loginUsername
                    profile.empty = true
                    profile.putStudentData("studentId", student1[0].toInt())
                    profileList.add(profile)
                }

                EventBus.getDefault().post(FirstLoginFinishedEvent(profileList, data.loginStore))
                onSuccess()
            }
        }
    }
}
