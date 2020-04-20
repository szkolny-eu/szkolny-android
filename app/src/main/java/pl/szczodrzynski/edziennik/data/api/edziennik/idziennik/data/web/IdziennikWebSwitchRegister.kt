package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_HOME
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.getString

class IdziennikWebSwitchRegister(override val data: DataIdziennik,
                                 val registerId: Int,
                                 val onSuccess: () -> Unit
) : IdziennikWeb(data, null) {
    companion object {
        private const val TAG = "IdziennikWebSwitchRegister"
    }

    init {
        val hiddenFields = data.loginStore.getLoginData("hiddenFields", JsonObject())
        // TODO error checking

        webGet(TAG, IDZIENNIK_WEB_HOME, mapOf(
                "__VIEWSTATE" to hiddenFields.getString("__VIEWSTATE", ""),
                "__VIEWSTATEGENERATOR" to hiddenFields.getString("__VIEWSTATEGENERATOR", ""),
                "__EVENTVALIDATION" to hiddenFields.getString("__EVENTVALIDATION", ""),
                "ctl00\$dxComboUczniowie" to registerId
        )) { text ->
            Regexes.IDZIENNIK_WEB_SELECTED_REGISTER.find(text)?.let {
                val registerId = it[1].toIntOrNull() ?: return@let
                data.webSelectedRegister = registerId
            }
            onSuccess()
        }
    }
}
