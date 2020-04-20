package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import pl.szczodrzynski.edziennik.utils.models.Time

class ConverterTime {
    @TypeConverter
    fun toTime(value: String?): Time? = when (value) {
        null -> null
        "null" -> null
        else -> Time.fromHms(value)
    }

    @TypeConverter
    fun toString(value: Time?): String? = value?.stringValue
}
