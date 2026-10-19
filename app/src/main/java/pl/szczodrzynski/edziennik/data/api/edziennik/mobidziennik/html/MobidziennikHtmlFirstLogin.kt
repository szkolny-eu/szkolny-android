package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.login.MobidziennikLogin
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.ext.set

class MobidziennikHtmlFirstLogin(val data: DataMobidziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "MobidziennikHtmlFirstLogin"
    }

    init {
        MobidziennikLogin.loginWeb(data) {
            val name = HtmlPlaceholder.studentName(data)
            val profile = Profile(
                data.loginStore.id,
                data.loginStore.id,
                LoginType.MOBIDZIENNIK,
                name,
                data.loginUsername,
                name,
                name,
                null,
            ).apply {
                studentData["studentId"] = HtmlPlaceholder.STUDENT_ID
            }

            EventBus.getDefault().postSticky(FirstLoginFinishedEvent(listOf(profile), data.loginStore))
            onSuccess()
        }
    }
}
