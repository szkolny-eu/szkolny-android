/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.entity

import android.os.Bundle
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.db.enums.LoginMode
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.ext.*

@Entity(tableName = "loginStores", primaryKeys = ["loginStoreId"])
class LoginStore(
        @ColumnInfo(name = "loginStoreId")
        val id: Int,

        @ColumnInfo(name = "loginStoreType")
        val type: LoginType,

        @ColumnInfo(name = "loginStoreMode")
        val mode: LoginMode,

        @ColumnInfo(name = "loginStoreData")
        val data: JsonObject = JsonObject()
) {

    fun hasLoginData(key: String) = data.has(key)
    fun getLoginData(key: String, defaultValue: Boolean) = data.getBoolean(key) ?: defaultValue
    fun getLoginData(key: String, defaultValue: String?) = data.getString(key) ?: defaultValue
    fun getLoginData(key: String, defaultValue: Int) = data.getInt(key) ?: defaultValue
    fun getLoginData(key: String, defaultValue: Long) = data.getLong(key) ?: defaultValue
    fun getLoginData(key: String, defaultValue: Float) = data.getFloat(key) ?: defaultValue
    fun getLoginData(key: String, defaultValue: Char) = data.getChar(key) ?: defaultValue
    fun getLoginData(key: String, defaultValue: JsonObject) = data.getJsonObject(key) ?: defaultValue
    fun putLoginData(key: String, value: Boolean) { data[key] = value }
    fun putLoginData(key: String, value: String?) { data[key] = value }
    fun putLoginData(key: String, value: Number) { data[key] = value }
    fun putLoginData(key: String, value: Char) { data[key] = value }
    fun putLoginData(key: String, value: JsonObject) { data[key] = value }
    fun removeLoginData(key: String) { data.remove(key) }

    fun copyFrom(args: Bundle) {
        for (key in args.keySet()) {
            when (val o = args[key]) {
                is String -> putLoginData(key, o)
                is Int -> putLoginData(key, o)
                is Long -> putLoginData(key, o)
                is Float -> putLoginData(key, o)
                is Boolean -> putLoginData(key, o)
                is Bundle -> putLoginData(key, o.toJsonObject())
            }
        }
    }

    override fun toString(): String {
        return "LoginStore{" +
                "id=" + id +
                ", type=" + type +
                ", mode=" + mode +
                ", data=" + data +
                '}'
    }
}
