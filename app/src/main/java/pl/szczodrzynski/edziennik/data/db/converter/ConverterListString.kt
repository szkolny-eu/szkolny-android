package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConverterListString {
    @TypeConverter
    fun toListString(value: String?): List<String>? = value?.let { Gson().fromJson<List<String>>(it, object : TypeToken<List<String?>?>() {}.type) }

    @TypeConverter
    fun toString(value: List<String?>?): String? = value?.let { Gson().toJson(it) }
}
