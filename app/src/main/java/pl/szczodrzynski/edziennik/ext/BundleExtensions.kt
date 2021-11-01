/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.google.gson.JsonObject

fun Bundle?.getInt(key: String, defaultValue: Int): Int {
    return this?.getInt(key, defaultValue) ?: defaultValue
}
fun Bundle?.getLong(key: String, defaultValue: Long): Long {
    return this?.getLong(key, defaultValue) ?: defaultValue
}
fun Bundle?.getFloat(key: String, defaultValue: Float): Float {
    return this?.getFloat(key, defaultValue) ?: defaultValue
}
fun Bundle?.getString(key: String, defaultValue: String): String {
    return this?.getString(key, defaultValue) ?: defaultValue
}

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
            when (property.second) {
                is String -> putString(property.first, property.second as String?)
                is Char -> putChar(property.first, property.second as Char)
                is Int -> putInt(property.first, property.second as Int)
                is Long -> putLong(property.first, property.second as Long)
                is Float -> putFloat(property.first, property.second as Float)
                is Short -> putShort(property.first, property.second as Short)
                is Double -> putDouble(property.first, property.second as Double)
                is Boolean -> putBoolean(property.first, property.second as Boolean)
                is Bundle -> putBundle(property.first, property.second as Bundle)
                is Parcelable -> putParcelable(property.first, property.second as Parcelable)
                is Array<*> -> putParcelableArray(property.first, property.second as Array<out Parcelable>)
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
