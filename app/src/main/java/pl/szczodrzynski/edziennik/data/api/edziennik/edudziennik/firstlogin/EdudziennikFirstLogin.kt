/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.firstlogin

import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikApi
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

class EdudziennikFirstLogin(val data: DataEdudziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "TemplateFirstLogin"
    }

    private val web = EdudziennikWeb(data)
    private val api = EdudziennikApi(data)
    private val profileList = mutableListOf<Profile>()

    init {
        /*TemplateLoginWeb(data) {
            web.webGet(TAG, "get all accounts") { text ->
                //val accounts = json.getJsonArray("accounts")

                EventBus.getDefault().post(FirstLoginFinishedEvent(profileList, data.loginStore))
                onSuccess()
            }
        }*/
    }
}
