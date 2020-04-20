package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class ConverterJsonObject {
    @TypeConverter
    fun toJsonObject(value: String?): JsonObject? = value?.let { JsonParser().parse(it).asJsonObject }

    @TypeConverter
    fun toString(value: JsonObject?): String? = value?.toString()
}
