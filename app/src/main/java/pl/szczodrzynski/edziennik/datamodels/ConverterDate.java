package pl.szczodrzynski.edziennik.datamodels;

import androidx.room.TypeConverter;

import pl.szczodrzynski.edziennik.models.Date;

public class ConverterDate {

    @TypeConverter
    public static Date toDate(String value) {
        return value == null ? null : Date.fromY_m_d(value);
    }

    @TypeConverter
    public static String toString(Date value) {
        return value == null ? null : value.getStringY_m_d();
    }
}
