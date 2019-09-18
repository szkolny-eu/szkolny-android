package pl.szczodrzynski.edziennik.datamodels;

import androidx.room.TypeConverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConverterJsonObject {

    @TypeConverter
    public static JsonObject toJsonObject(String value) {
        return value == null ? null : new JsonParser().parse(value).getAsJsonObject();
    }

    @TypeConverter
    public static String toString(JsonObject value) {
        return value == null ? null : value.toString();
    }
}
