/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.debug.models

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.ui.grades.models.ExpandableItemModel

data class LabJsonObject(
        val key: String,
        val jsonObject: JsonObject,
        override var level: Int
) : ExpandableItemModel<JsonElement>(jsonObject.entrySet().map { it.value }.toMutableList())
