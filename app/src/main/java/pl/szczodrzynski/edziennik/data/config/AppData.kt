/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-21.
 */

package pl.szczodrzynski.edziennik.data.config

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.ext.getJsonObject
import pl.szczodrzynski.edziennik.ext.mergeWith
import pl.szczodrzynski.edziennik.utils.managers.TextStylingManager.HtmlMode

data class AppData(
    val configOverrides: Map<String, String>,
    val messagesConfig: MessagesConfig,
    val uiConfig: UIConfig,
    val eventTypes: List<EventType>,
) {
    companion object {
        private var data: JsonObject? = null
        private val appData = mutableMapOf<LoginType, AppData>()

        fun read(app: App) {
            val res = app.resources.openRawResource(R.raw.app_data)
            data = JsonParser.parseReader(JsonReader(res.reader())).asJsonObject
        }

        fun get(loginType: LoginType): AppData {
            if (loginType in appData)
                return appData.getValue(loginType)
            val json = data?.getJsonObject("base")?.deepCopy()
                ?: throw NoSuchElementException("Base data not found")
            val overrides = setOf(loginType, loginType.schoolType)
            for (overrideType in overrides) {
                val override = data?.getJsonObject(overrideType.name.lowercase()) ?: continue
                json.mergeWith(override)
            }
            val value = Gson().fromJson(json, AppData::class.java)
            appData[loginType] = value
            return value
        }
    }

    data class MessagesConfig(
        val subjectLength: Int?,
        val bodyLength: Int?,
        val textStyling: Boolean,
        val syncRecipientList: Boolean,
        val htmlMode: HtmlMode,
        val needsReadStatus: Boolean,
    )

    data class UIConfig(
        val lessonHeight: Int,
        val enableMarkAsReadAnnouncements: Boolean,
        val enableNoticePoints: Boolean,
        val eventManualShowSubjectDropdown: Boolean,
    )

    data class EventType(
        val id: Long,
        val color: String,
        val name: String,
    )
}
