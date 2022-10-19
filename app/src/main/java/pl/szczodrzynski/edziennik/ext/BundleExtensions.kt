/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.db.enums.*
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget

fun Bundle?.getInt(key: String, defaultValue: Int) =
    this?.getInt(key, defaultValue) ?: defaultValue

fun Bundle?.getLong(key: String, defaultValue: Long) =
    this?.getLong(key, defaultValue) ?: defaultValue

fun Bundle?.getFloat(key: String, defaultValue: Float) =
    this?.getFloat(key, defaultValue) ?: defaultValue

fun Bundle?.getString(key: String, defaultValue: String) =
    this?.getString(key, defaultValue) ?: defaultValue

inline fun <reified E> Bundle?.getEnum(key: String) = this?.getInt(key)?.toEnum<E>()
fun Bundle.putEnum(key: String, value: Enum<*>) = putInt(key, value.toInt())

fun Bundle?.getIntOrNull(key: String): Int? {
    return this?.get(key) as? Int
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> Bundle?.get(key: String): T? {
    return this?.get(key) as? T?
}

@Suppress("UNCHECKED_CAST")
fun Bundle(vararg properties: Pair<String, Any?>): Bundle {
    return Bundle().apply {
        for (property in properties) {
            val (key, value) = property
            when (value) {
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
                is Parcelable -> putParcelable(key, value)
                is Array<*> -> putParcelableArray(key, value as Array<out Parcelable>)
            }
        }
    }
}

fun Intent(action: String? = null, vararg properties: Pair<String, Any?>): Intent {
    return Intent(action).putExtras(Bundle(*properties))
}

fun Intent(packageContext: Context, cls: Class<*>, vararg properties: Pair<String, Any?>): Intent {
    return Intent(packageContext, cls).putExtras(Bundle(*properties))
}

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
