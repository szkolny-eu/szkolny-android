package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import pl.szczodrzynski.edziennik.utils.models.Date

class ConverterDate {
    @TypeConverter
    fun toDate(value: String?): Date? = value?.let { Date.fromY_m_d(it) }

    @TypeConverter
    fun toString(value: Date?): String? = value?.stringY_m_d
}
