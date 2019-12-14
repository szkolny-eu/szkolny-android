package pl.szczodrzynski.edziennik.data.db.modules.login;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull;

@Entity(tableName = "loginStores",
        primaryKeys = {"loginStoreId"})
public class LoginStore {
    @ColumnInfo(name = "loginStoreId")
    public int id = -1;
    @ColumnInfo(name = "loginStoreType")
    public int type = -1;
    public final static int LOGIN_TYPE_MOBIDZIENNIK = 1;
    public final static int LOGIN_TYPE_LIBRUS = 2;
    public final static int LOGIN_TYPE_VULCAN = 4;
    public final static int LOGIN_TYPE_IUCZNIOWIE = 3;
    public final static int LOGIN_TYPE_DEMO = 20;
    @ColumnInfo(name = "loginStoreData")
    public JsonObject data;

    @ColumnInfo(name = "loginStoreMode")
    public int mode = 0;
    public static final int LOGIN_MODE_LIBRUS_EMAIL = 0;
    public static final int LOGIN_MODE_LIBRUS_SYNERGIA = 1;
    public static final int LOGIN_MODE_LIBRUS_JST = 2;

    public LoginStore(int id, int type, JsonObject data) {
        this.id = id;
        this.type = type;
        this.data = data;
    }

    public static LoginStore fromProfileFull(ProfileFull profileFull) {
        return new LoginStore(profileFull.getLoginStoreId(), profileFull.getLoginStoreType(), profileFull.getLoginStoreData());
    }

    public void copyFrom(Bundle args) {
        for (String key: args.keySet()) {
            Object o = args.get(key);
            if (o instanceof String) {
                putLoginData(key, (String) o);
            }
            else if (o instanceof Integer) {
                putLoginData(key, (Integer) o);
            }
            else if (o instanceof Long) {
                putLoginData(key, (Long) o);
            }
            else if (o instanceof Float) {
                putLoginData(key, (Float) o);
            }
            else if (o instanceof Boolean) {
                putLoginData(key, (Boolean) o);
            }
        }
    }
    public boolean hasLoginData(String key) {
        if (data == null)
            return false;
        return data.has(key);
    }
    @Nullable
    public String getLoginData(String key, @Nullable String defaultValue) {
        if (data == null)
            return defaultValue;
        JsonElement element = data.get(key);
        if (element != null && !(element instanceof JsonNull)) {
            return element.getAsString();
        }
        return defaultValue;
    }
    @Nullable
    public int getLoginData(String key, int defaultValue) {
        if (data == null)
            return defaultValue;
        JsonElement element = data.get(key);
        if (element != null && !(element instanceof JsonNull)) {
            return element.getAsInt();
        }
        return defaultValue;
    }
    @Nullable
    public long getLoginData(String key, long defaultValue) {
        if (data == null)
            return defaultValue;
        JsonElement element = data.get(key);
        if (element != null && !(element instanceof JsonNull)) {
            return element.getAsLong();
        }
        return defaultValue;
    }
    @Nullable
    public float getLoginData(String key, float defaultValue) {
        if (data == null)
            return defaultValue;
        JsonElement element = data.get(key);
        if (element != null && !(element instanceof JsonNull)) {
            return element.getAsFloat();
        }
        return defaultValue;
    }
    public boolean getLoginData(String key, boolean defaultValue) {
        if (data == null)
            return defaultValue;
        JsonElement element = data.get(key);
        if (element != null && !(element instanceof JsonNull)) {
            return element.getAsBoolean();
        }
        return defaultValue;
    }

    public void putLoginData(String key, String value) {
        forceLoginStore();
        data.addProperty(key, value);
    }
    public void putLoginData(String key, int value) {
        forceLoginStore();
        data.addProperty(key, value);
    }
    public void putLoginData(String key, long value) {
        forceLoginStore();
        data.addProperty(key, value);
    }
    public void putLoginData(String key, float value) {
        forceLoginStore();
        data.addProperty(key, value);
    }
    public void putLoginData(String key, boolean value) {
        forceLoginStore();
        data.addProperty(key, value);
    }

    public void removeLoginData(String key) {
        if (data == null)
            return;
        data.remove(key);
    }

    public void clearLoginStore() {
        data = new JsonObject();
    }

    private void forceLoginStore() {
        if (data == null) {
            clearLoginStore();
        }
    }

    public String type() {
        switch (type) {
            case LOGIN_TYPE_MOBIDZIENNIK:
                return "LOGIN_TYPE_MOBIDZIENNIK";
            case LOGIN_TYPE_LIBRUS:
                return "LOGIN_TYPE_LIBRUS";
            case LOGIN_TYPE_IUCZNIOWIE:
                return "LOGIN_TYPE_IDZIENNIK";
            case LOGIN_TYPE_VULCAN:
                return "LOGIN_TYPE_VULCAN";
            case LOGIN_TYPE_DEMO:
                return "LOGIN_TYPE_DEMO";
            default:
                return "unknown";
        }
    }
    public String mode() {
        switch (mode) {
            case LOGIN_MODE_LIBRUS_EMAIL:
                return "LOGIN_MODE_LIBRUS_EMAIL";
            case LOGIN_MODE_LIBRUS_SYNERGIA:
                return "LOGIN_MODE_LIBRUS_SYNERGIA";
            case LOGIN_MODE_LIBRUS_JST:
                return "LOGIN_MODE_LIBRUS_JST";
            default:
                return "unknown";
        }
    }

    @Override
    public String toString() {
        return "LoginStore{" +
                "id=" + id +
                ", type=" + type() +
                ", mode=" + mode() +
                ", data=" + data +
                '}';
    }
}
