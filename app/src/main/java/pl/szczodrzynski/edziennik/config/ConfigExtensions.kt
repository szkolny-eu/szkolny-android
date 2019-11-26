/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

private val gson = Gson()

fun Config.set(profileId: Int, key: String, value: Int) {
    set(profileId, key, value.toString())
}
fun Config.set(profileId: Int, key: String, value: Boolean) {
    set(profileId, key, value.toString())
}
fun Config.set(profileId: Int, key: String, value: Long) {
    set(profileId, key, value.toString())
}
fun Config.set(profileId: Int, key: String, value: Float) {
    set(profileId, key, value.toString())
}
fun Config.set(profileId: Int, key: String, value: Date?) {
    set(profileId, key, value?.stringY_m_d)
}
fun Config.set(profileId: Int, key: String, value: Time?) {
    set(profileId, key, value?.stringValue)
}
fun Config.set(profileId: Int, key: String, value: JsonElement?) {
    set(profileId, key, value?.toString())
}
fun Config.set(profileId: Int, key: String, value: List<Any>?) {
    set(profileId, key, value?.let { gson.toJson(it) })
}
fun Config.setStringList(profileId: Int, key: String, value: List<String>?) {
    set(profileId, key, value?.let { gson.toJson(it) })
}
fun Config.setIntList(profileId: Int, key: String, value: List<Int>?) {
    set(profileId, key, value?.let { gson.toJson(it) })
}
fun Config.setLongList(profileId: Int, key: String, value: List<Long>?) {
    set(profileId, key, value?.let { gson.toJson(it) })
}

fun HashMap<String, String?>.get(key: String, default: String?): String? {
    return this[key] ?: default
}
fun HashMap<String, String?>.get(key: String, default: Boolean): Boolean {
    return this[key]?.toBoolean() ?: default
}
fun HashMap<String, String?>.get(key: String, default: Int): Int {
    return this[key]?.toIntOrNull() ?: default
}
fun HashMap<String, String?>.get(key: String, default: Long): Long {
    return this[key]?.toLongOrNull() ?: default
}
fun HashMap<String, String?>.get(key: String, default: Float): Float {
    return this[key]?.toFloatOrNull() ?: default
}
fun HashMap<String, String?>.get(key: String, default: Date?): Date? {
    return this[key]?.let { Date.fromY_m_d(it) } ?: default
}
fun HashMap<String, String?>.get(key: String, default: Time?): Time? {
    return this[key]?.let { Time.fromHms(it) } ?: default
}
fun HashMap<String, String?>.get(key: String, default: JsonObject?): JsonObject? {
    return this[key]?.let { JsonParser().parse(it)?.asJsonObject } ?: default
}
fun HashMap<String, String?>.get(key: String, default: JsonArray?): JsonArray? {
    return this[key]?.let { JsonParser().parse(it)?.asJsonArray } ?: default
}
fun <T> HashMap<String, String?>.get(key: String, default: List<T>?): List<T>? {
    return this[key]?.let { gson.fromJson<List<T>>(it, object: TypeToken<List<T>>(){}.type) } ?: default
}
fun HashMap<String, String?>.getStringList(key: String, default: List<String>?): List<String>? {
    return this[key]?.let { gson.fromJson<List<String>>(it, object: TypeToken<List<String>>(){}.type) } ?: default
}
fun HashMap<String, String?>.getIntList(key: String, default: List<Int>?): List<Int>? {
    return this[key]?.let { gson.fromJson<List<Int>>(it, object: TypeToken<List<Int>>(){}.type) } ?: default
}
fun HashMap<String, String?>.getLongList(key: String, default: List<Long>?): List<Long>? {
    return this[key]?.let { gson.fromJson<List<Long>>(it, object: TypeToken<List<Long>>(){}.type) } ?: default
}

fun List<ConfigEntry>.toHashMap(map: HashMap<String, String?>) {
    map.clear()
    forEach {
        map[it.key] = it.value
    }
}