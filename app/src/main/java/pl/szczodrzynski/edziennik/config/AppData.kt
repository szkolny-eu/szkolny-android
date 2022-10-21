/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-21.
 */

package pl.szczodrzynski.edziennik.config

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
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

        fun read(app: App, profile: Profile = app.profile): AppData {
            if (data == null) {
                val res = app.resources.openRawResource(R.raw.app_data)
                data = JsonParser.parseReader(JsonReader(res.reader())).asJsonObject
            }
            val json = data?.getJsonObject("base")?.deepCopy()
                ?: throw NoSuchElementException("Base data not found")
            val loginType = profile.loginStoreType
            val overrides = setOf(loginType, loginType.schoolType)
            for (overrideType in overrides) {
                val override = data?.getJsonObject(overrideType.name.lowercase()) ?: continue
                json.mergeWith(override)
            }
            return app.gson.fromJson(json, AppData::class.java)
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
    )

    data class EventType(
        val id: Int,
        val color: String,
        val name: String,
    )
}
