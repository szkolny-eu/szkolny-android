/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-27. 
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.template.firstlogin

import pl.szczodrzynski.edziennik.api.v2.edziennik.template.DataTemplate
import pl.szczodrzynski.edziennik.api.v2.edziennik.template.data.TemplateApi
import pl.szczodrzynski.edziennik.api.v2.edziennik.template.data.TemplateWeb
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

class TemplateFirstLogin(val data: DataTemplate, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "TemplateFirstLogin"
    }

    private val web = TemplateWeb(data)
    private val api = TemplateApi(data)
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
