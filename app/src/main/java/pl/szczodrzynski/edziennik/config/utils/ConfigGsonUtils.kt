/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-2.
 */
package pl.szczodrzynski.edziennik.config.utils

import com.google.gson.Gson
import com.google.gson.JsonParser
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ui.home.HomeCardModel
import pl.szczodrzynski.edziennik.utils.models.Time

class ConfigGsonUtils {
    @Suppress("UNCHECKED_CAST")
    fun <T> deserializeList(gson: Gson, str: String?, classOfT: Class<T>): List<T> {
        val json = JsonParser.parseString(str)
        val list: MutableList<T> = mutableListOf()
        if (!json.isJsonArray)
            return list

        json.asJsonArray.forEach { e ->
            when (classOfT) {
                String::class.java -> {
                    list += e.asString as T
                }
                HomeCardModel::class.java -> {
                    val o = e.asJsonObject
                    list += HomeCardModel(
                            o.getInt("profileId", 0),
                            o.getInt("cardId", 0)
                    ) as T
                }
                Time::class.java -> {
                    val o = e.asJsonObject
                    list += Time(
                            o.getInt("hour", 0),
                            o.getInt("minute", 0),
                            o.getInt("second", 0)
                    ) as T
                }
            }
        }

        return list
    }
}
