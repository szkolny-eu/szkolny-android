/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.converters;

import androidx.room.TypeConverter;

import pl.szczodrzynski.edziennik.utils.models.Date;

public class ConverterDateInt {

    @TypeConverter
    public static Date toDate(int value) {
        return Date.fromYmd(Integer.toString(value));
    }

    @TypeConverter
    public static int toInt(Date value) {
        return value == null ? 0 : value.getValue();
    }
}
