/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.google.gson.JsonObject
import java.io.Serializable

fun Bundle?.getInt(key: String, defaultValue: Int) =
    this?.getInt(key, defaultValue) ?: defaultValue

fun Bundle?.getLong(key: String, defaultValue: Long) =
    this?.getLong(key, defaultValue) ?: defaultValue

fun Bundle?.getFloat(key: String, defaultValue: Float) =
    this?.getFloat(key, defaultValue) ?: defaultValue

fun Bundle?.getString(key: String, defaultValue: String) =
    this?.getString(key, defaultValue) ?: defaultValue

inline fun <reified E : Enum<E>> Bundle?.getEnum(key: String) = this?.getString(key)?.toEnumOrNull<E>()
fun Bundle.putEnum(key: String, value: Enum<*>) = putString(key, value.toString())

fun Bundle?.getIntOrNull(key: String): Int? {
    return this?.get(key) as? Int
}

@Suppress("UNCHECKED_CAST")
operator fun Bundle.set(key: String, value: Any) = when (value) {
    is String -> putString(key, value as String?)
    is Char -> putChar(key, value)
    is Int -> putInt(key, value)
    is Long -> putLong(key, value)
    is Float -> putFloat(key, value)
    is Short -> putShort(key, value)
    is Double -> putDouble(key, value)
    is Boolean -> putBoolean(key, value)
    is Bundle -> putBundle(key, value)
    is Enum<*> -> putEnum(key, value)
    is Array<*> -> putParcelableArray(key, value as Array<out Parcelable>)
    is Parcelable -> putParcelable(key, value)
    is Serializable -> putSerializable(key, value)
    else -> throw IllegalArgumentException("Couldn't serialize $key = $value")
}

fun Bundle.putExtras(vararg properties: Pair<String, Any?>): Bundle {
    for (property in properties) {
        val (key, value) = property
        this[key] = value ?: continue
    }
    return this
}

fun Bundle(vararg properties: Pair<String, Any?>) = Bundle().putExtras(*properties)

fun Intent(action: String? = null, vararg properties: Pair<String, Any?>): Intent {
    return Intent(action).putExtras(Bundle(*properties))
}

fun Intent(packageContext: Context, cls: Class<*>, vararg properties: Pair<String, Any?>): Intent {
    return Intent(packageContext, cls).putExtras(Bundle(*properties))
}

fun Intent.putExtras(vararg properties: Pair<String, Any?>) = putExtras(Bundle(*properties))


fun Bundle.toJsonObject(): JsonObject {
    val json = JsonObject()
    keySet()?.forEach { key ->
        get(key)?.let {
            when (it) {
                is String -> json.addProperty(key, it)
                is Char -> json.addProperty(key, it)
                is Int -> json.addProperty(key, it)
                is Long -> json.addProperty(key, it)
                is Float -> json.addProperty(key, it)
                is Short -> json.addProperty(key, it)
                is Double -> json.addProperty(key, it)
                is Boolean -> json.addProperty(key, it)
                is Bundle -> json.add(key, it.toJsonObject())
                else -> json.addProperty(key, it.toString())
            }
        }
    }
    return json
}

fun Intent.toJsonObject() = extras?.toJsonObject()
