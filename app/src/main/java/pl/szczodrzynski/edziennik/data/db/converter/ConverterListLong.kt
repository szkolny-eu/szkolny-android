package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConverterListLong {
    @TypeConverter
    fun toListLong(value: String?): List<Long>? = value?.let { Gson().fromJson<List<Long>>(it, object : TypeToken<List<Long?>?>() {}.type) }

    @TypeConverter
    fun toString(value: List<Long?>?): String? = value?.let { Gson().toJson(it) }
}
