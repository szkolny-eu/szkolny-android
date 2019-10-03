package pl.szczodrzynski.edziennik.data.db.converters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import androidx.room.TypeConverter;

public class ConverterListString {

    @TypeConverter
    public static List<String> toListString(String value) {
        return value == null ? null : new Gson().fromJson(value, new TypeToken<List<String>>(){}.getType());
    }

    @TypeConverter
    public static String toString(List<String> value) {
        return value == null ? null : new Gson().toJson(value);
    }
}
