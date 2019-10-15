package pl.szczodrzynski.edziennik.api.v2.mobidziennik.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.api.v2.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.login.MobidziennikLoginWeb
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

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

                val studentIds = mutableListOf<Int>()
                val studentNamesLong = mutableListOf<String>()
                val studentNamesShort = mutableListOf<String>()
                val student = tables[8].split("\n")

                for (aStudent in student) {
                    if (aStudent.isEmpty())
                        continue
                    val student1 = aStudent.split("|")
                    if (student1.size == 2)
                        continue
                    studentIds += student1[0].toInt()
                    studentNamesLong.add(student1[2] + " " + student1[4])
                    studentNamesShort.add(student1[2] + " " + student1[4][0] + ".")
                }

                for (index in studentIds.indices) {
                    val profile = Profile()
                    profile.studentNameLong = studentNamesLong[index]
                    profile.studentNameShort = studentNamesShort[index]
                    profile.name = profile.studentNameLong
                    profile.subname = data.loginUsername
                    profile.empty = true
                    profile.putStudentData("studentId", studentIds[index])
                    profileList.add(profile)
                }

                EventBus.getDefault().post(FirstLoginFinishedEvent(profileList, data.loginStore))
                onSuccess()
            }
        }
    }
}