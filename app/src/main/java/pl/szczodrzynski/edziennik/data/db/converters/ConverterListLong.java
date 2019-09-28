package pl.szczodrzynski.edziennik.data.db.converters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import androidx.room.TypeConverter;

public class ConverterListLong {

    @TypeConverter
    public static List<Long> toListLong(String value) {
        return value == null ? null : new Gson().fromJson(value, new TypeToken<List<Long>>(){}.getType());
    }

    @TypeConverter
    public static String toString(List<Long> value) {
        return value == null ? null : new Gson().toJson(value);
    }
}

