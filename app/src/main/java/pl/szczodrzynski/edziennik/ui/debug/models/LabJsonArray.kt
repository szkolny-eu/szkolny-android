/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.debug.models

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import pl.szczodrzynski.edziennik.ui.grades.models.ExpandableItemModel

data class LabJsonArray(
        val key: String,
        val jsonArray: JsonArray,
        override var level: Int
) : ExpandableItemModel<JsonElement>(jsonArray.toMutableList())
