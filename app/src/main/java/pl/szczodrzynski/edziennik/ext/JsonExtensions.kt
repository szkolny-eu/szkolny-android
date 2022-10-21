/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.os.Bundle
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun JsonObject?.get(key: String): JsonElement? = this?.get(key)

fun JsonObject?.getBoolean(key: String): Boolean? = get(key)?.let { if (!it.isJsonPrimitive) null else it.asBoolean }
fun JsonObject?.getString(key: String): String? = get(key)?.let { if (!it.isJsonPrimitive) null else it.asString }
fun JsonObject?.getInt(key: String): Int? = get(key)?.let { if (!it.isJsonPrimitive) null else it.asInt }
fun JsonObject?.getLong(key: String): Long? = get(key)?.let { if (!it.isJsonPrimitive) null else it.asLong }
fun JsonObject?.getFloat(key: String): Float? = get(key)?.let { if(!it.isJsonPrimitive) null else it.asFloat }
fun JsonObject?.getChar(key: String): Char? = get(key)?.let { if(!it.isJsonPrimitive) null else it.asString[0] }
fun JsonObject?.getJsonObject(key: String): JsonObject? = get(key)?.let { if (it.isJsonObject) it.asJsonObject else null }
fun JsonObject?.getJsonArray(key: String): JsonArray? = get(key)?.let { if (it.isJsonArray) it.asJsonArray else null }

fun JsonObject?.getBoolean(key: String, defaultValue: Boolean): Boolean = get(key)?.let { if (!it.isJsonPrimitive) defaultValue else it.asBoolean } ?: defaultValue
fun JsonObject?.getString(key: String, defaultValue: String): String = get(key)?.let { if (!it.isJsonPrimitive) defaultValue else it.asString } ?: defaultValue
fun JsonObject?.getInt(key: String, defaultValue: Int): Int = get(key)?.let { if (!it.isJsonPrimitive) defaultValue else it.asInt } ?: defaultValue
fun JsonObject?.getLong(key: String, defaultValue: Long): Long = get(key)?.let { if (!it.isJsonPrimitive) defaultValue else it.asLong } ?: defaultValue
fun JsonObject?.getFloat(key: String, defaultValue: Float): Float = get(key)?.let { if(!it.isJsonPrimitive) defaultValue else it.asFloat } ?: defaultValue
fun JsonObject?.getChar(key: String, defaultValue: Char): Char = get(key)?.let { if(!it.isJsonPrimitive) defaultValue else it.asString[0] } ?: defaultValue
fun JsonObject?.getJsonObject(key: String, defaultValue: JsonObject): JsonObject = get(key)?.let { if (it.isJsonObject) it.asJsonObject else defaultValue } ?: defaultValue
fun JsonObject?.getJsonArray(key: String, defaultValue: JsonArray): JsonArray = get(key)?.let { if (it.isJsonArray) it.asJsonArray else defaultValue } ?: defaultValue

fun JsonArray.getBoolean(key: Int): Boolean? = if (key >= size()) null else get(key)?.let { if (!it.isJsonPrimitive) null else it.asBoolean }
fun JsonArray.getString(key: Int): String? = if (key >= size()) null else get(key)?.let { if (!it.isJsonPrimitive) null else it.asString }
fun JsonArray.getInt(key: Int): Int? = if (key >= size()) null else get(key)?.let { if (!it.isJsonPrimitive) null else it.asInt }
fun JsonArray.getLong(key: Int): Long? = if (key >= size()) null else get(key)?.let { if (!it.isJsonPrimitive) null else it.asLong }
fun JsonArray.getFloat(key: Int): Float? = if (key >= size()) null else get(key)?.let { if(!it.isJsonPrimitive) null else it.asFloat }
fun JsonArray.getChar(key: Int): Char? = if (key >= size()) null else get(key)?.let { if(!it.isJsonPrimitive) null else it.asString[0] }
fun JsonArray.getJsonObject(key: Int): JsonObject? = if (key >= size()) null else get(key)?.let { if (it.isJsonObject) it.asJsonObject else null }
fun JsonArray.getJsonArray(key: Int): JsonArray? = if (key >= size()) null else get(key)?.let { if (it.isJsonArray) it.asJsonArray else null }

inline fun <reified E : Enum<E>> JsonObject?.getEnum(key: String) = this?.getInt(key)?.toEnum<E>()
fun JsonObject.putEnum(key: String, value: Enum<*>) = addProperty(key, value.toInt())

fun String.toJsonObject(): JsonObject? = try { JsonParser.parseString(this).asJsonObject } catch (ignore: Exception) { null }

operator fun JsonObject.set(key: String, value: JsonElement) = this.add(key, value)
operator fun JsonObject.set(key: String, value: Boolean) = this.addProperty(key, value)
operator fun JsonObject.set(key: String, value: String?) = this.addProperty(key, value)
operator fun JsonObject.set(key: String, value: Number) = this.addProperty(key, value)
operator fun JsonObject.set(key: String, value: Char) = this.addProperty(key, value)

fun JsonArray.asJsonObjectList() = this.mapNotNull { it.asJsonObject }

fun JsonObject(vararg properties: Pair<String, Any?>): JsonObject {
    return JsonObject().apply {
        for (property in properties) {
            val (key, value) = property
            when (value) {
                is JsonElement -> add(key, value)
                is String -> addProperty(key, value)
                is Char -> addProperty(key, value)
                is Number -> addProperty(key, value)
                is Boolean -> addProperty(key, value)
                is Enum<*> -> addProperty(key, value.toInt())
            }
        }
    }
}

fun JsonObject.toBundle(): Bundle {
    return Bundle().also {
        for ((key, value) in this.entrySet()) {
            when (value) {
                is JsonObject -> it.putBundle(key, value.toBundle())
                is JsonPrimitive -> when {
                    value.isString -> it.putString(key, value.asString)
                    value.isBoolean -> it.putBoolean(key, value.asBoolean)
                    value.isNumber -> it.putInt(key, value.asInt)
                }
            }
        }
    }
}

fun JsonArray(vararg properties: Any?): JsonArray {
    return JsonArray().apply {
        for (property in properties) {
            when (property) {
                is JsonElement -> add(property as JsonElement?)
                is String -> add(property as String?)
                is Char -> add(property as Char?)
                is Number -> add(property as Number?)
                is Boolean -> add(property as Boolean?)
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun JsonArray?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this == null || this.isEmpty
}
operator fun JsonArray.plusAssign(o: JsonElement) = this.add(o)
operator fun JsonArray.plusAssign(o: String) = this.add(o)
operator fun JsonArray.plusAssign(o: Char) = this.add(o)
operator fun JsonArray.plusAssign(o: Number) = this.add(o)
operator fun JsonArray.plusAssign(o: Boolean) = this.add(o)

fun JsonObject.mergeWith(other: JsonObject): JsonObject {
    for ((key, value) in other.entrySet()) {
        when (value) {
            is JsonObject -> when {
                this.has(key) -> this.getJsonObject(key)?.mergeWith(value)
                else -> this.add(key, value)
            }
            is JsonArray -> when {
                this.has(key) -> this.getJsonArray(key)?.addAll(value)
                else -> this.add(key, value)
            }
            else -> this.add(key, value)
        }
    }
    return this
}
