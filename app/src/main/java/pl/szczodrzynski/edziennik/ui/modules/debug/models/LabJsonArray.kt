/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.modules.debug.models

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import pl.szczodrzynski.edziennik.ui.modules.grades.models.ExpandableItemModel

data class LabJsonArray(
        val key: String,
        val jsonArray: JsonArray,
        override var level: Int
) : ExpandableItemModel<JsonElement>(jsonArray.toMutableList())
