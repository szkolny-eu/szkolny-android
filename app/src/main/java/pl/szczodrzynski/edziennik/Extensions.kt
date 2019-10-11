package pl.szczodrzynski.edziennik

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.LongSparseArray
import android.util.SparseArray
import androidx.core.app.ActivityCompat
import androidx.core.util.forEach
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import im.wangchao.mhttp.Response
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team
import pl.szczodrzynski.navlib.R
import pl.szczodrzynski.navlib.crc16
import pl.szczodrzynski.navlib.getColorFromRes
import java.text.SimpleDateFormat
import java.util.*


fun List<Teacher>.byId(id: Long) = firstOrNull { it.id == id }
fun List<Teacher>.byNameFirstLast(nameFirstLast: String) = firstOrNull { it.name + " " + it.surname == nameFirstLast }
fun List<Teacher>.byNameLastFirst(nameLastFirst: String) = firstOrNull { it.surname + " " + it.name == nameLastFirst }
fun List<Teacher>.byNameFDotLast(nameFDotLast: String) = firstOrNull { it.name + "." + it.surname == nameFDotLast }
fun List<Teacher>.byNameFDotSpaceLast(nameFDotSpaceLast: String) = firstOrNull { it.name + ". " + it.surname == nameFDotSpaceLast }

fun JsonObject?.get(key: String): JsonElement? = this?.get(key)

fun JsonObject?.getBoolean(key: String): Boolean? = get(key)?.let { if (it.isJsonNull) null else it.asBoolean }
fun JsonObject?.getString(key: String): String? = get(key)?.let { if (it.isJsonNull) null else it.asString }
fun JsonObject?.getInt(key: String): Int? = get(key)?.let { if (it.isJsonNull) null else it.asInt }
fun JsonObject?.getLong(key: String): Long? = get(key)?.let { if (it.isJsonNull) null else it.asLong }
fun JsonObject?.getJsonObject(key: String): JsonObject? = get(key)?.let { if (it.isJsonNull) null else it.asJsonObject }
fun JsonObject?.getJsonArray(key: String): JsonArray? = get(key)?.let { if (it.isJsonNull) null else it.asJsonArray }

fun JsonObject?.getBoolean(key: String, defaultValue: Boolean): Boolean = get(key)?.let { if (it.isJsonNull) defaultValue else it.asBoolean } ?: defaultValue
fun JsonObject?.getString(key: String, defaultValue: String): String = get(key)?.let { if (it.isJsonNull) defaultValue else it.asString } ?: defaultValue
fun JsonObject?.getInt(key: String, defaultValue: Int): Int = get(key)?.let { if (it.isJsonNull) defaultValue else it.asInt } ?: defaultValue
fun JsonObject?.getLong(key: String, defaultValue: Long): Long = get(key)?.let { if (it.isJsonNull) defaultValue else it.asLong } ?: defaultValue
fun JsonObject?.getJsonObject(key: String, defaultValue: JsonObject): JsonObject = get(key)?.let { if (it.isJsonNull) defaultValue else it.asJsonObject } ?: defaultValue
fun JsonObject?.getJsonArray(key: String, defaultValue: JsonArray): JsonArray = get(key)?.let { if (it.isJsonNull) defaultValue else it.asJsonArray } ?: defaultValue

fun CharSequence?.isNotNullNorEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

fun currentTimeUnix() = System.currentTimeMillis() / 1000

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

fun changeStringCase(s: String): String {
    val delimiters = " '-/"
    val sb = StringBuilder()
    var capNext = true
    for (ch in s.toCharArray()) {
        var c = ch
        c = if (capNext)
            Character.toUpperCase(c)
        else
            Character.toLowerCase(c)
        sb.append(c)
        capNext = delimiters.indexOf(c) >= 0
    }
    return sb.toString()
}

fun buildFullName(firstName: String?, lastName: String?): String {
    return changeStringCase("$firstName $lastName").trim()
}

fun colorFromName(context: Context, name: String?): Int {
    var crc = crc16(name ?: "")
    crc = (crc and 0xff) or (crc shr 8)
    crc %= 16
    val color = when (crc) {
        13 -> R.color.md_red_500
        4  -> R.color.md_pink_A400
        2  -> R.color.md_purple_A400
        9  -> R.color.md_deep_purple_A700
        5  -> R.color.md_indigo_500
        1  -> R.color.md_indigo_A700
        6  -> R.color.md_cyan_A200
        14 -> R.color.md_teal_400
        15 -> R.color.md_green_500
        7  -> R.color.md_yellow_A700
        3  -> R.color.md_deep_orange_A400
        8  -> R.color.md_deep_orange_A700
        10 -> R.color.md_brown_500
        12 -> R.color.md_grey_400
        11 -> R.color.md_blue_grey_400
        else -> R.color.md_light_green_A700
    }
    return context.getColorFromRes(color)
}

fun MutableList<out Profile>.filterOutArchived() {
    this.removeAll { it.archived }
}

fun Activity.isStoragePermissionGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= 23) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            false
        }
    } else {
        true
    }
}

fun Response?.getUnixDate(): Long {
    val rfcDate = this?.headers()?.get("date") ?: return currentTimeUnix()
    val pattern = "EEE, dd MMM yyyy HH:mm:ss Z"
    val format = SimpleDateFormat(pattern, Locale.ENGLISH)
    return format.parse(rfcDate).time / 1000
}

const val MINUTE = 60L
const val HOUR = 60L*MINUTE
const val DAY = 24L*HOUR
const val WEEK = 7L*DAY
const val MONTH = 30L*DAY
const val YEAR = 365L*DAY

fun <T> LongSparseArray<T>.values(): List<T> {
    val result = mutableListOf<T>()
    forEach { _, value ->
        result += value
    }
    return result
}

fun <T> SparseArray<T>.values(): List<T> {
    val result = mutableListOf<T>()
    forEach { _, value ->
        result += value
    }
    return result
}

fun <T> List<T>.toSparseArray(destination: SparseArray<T>, key: (T) -> Int) {
    forEach {
        destination.put(key(it), it)
    }
}
fun <T> List<T>.toSparseArray(destination: LongSparseArray<T>, key: (T) -> Long) {
    forEach {
        destination.put(key(it), it)
    }
}

fun <T> List<T>.toSparseArray(key: (T) -> Int): SparseArray<T> {
    val result = SparseArray<T>()
    toSparseArray(result, key)
    return result
}
fun <T> List<T>.toSparseArray(key: (T) -> Long): LongSparseArray<T> {
    val result = LongSparseArray<T>()
    toSparseArray(result, key)
    return result
}

fun <T> SparseArray<T>.singleOrNull(predicate: (T) -> Boolean): T? {
    forEach { _, value ->
        if (predicate(value))
            return value
    }
    return null
}
fun <T> LongSparseArray<T>.singleOrNull(predicate: (T) -> Boolean): T? {
    forEach { _, value ->
        if (predicate(value))
            return value
    }
    return null
}

fun String.fixWhiteSpaces() = buildString(length) {
    var wasWhiteSpace = true
    for (c in this@fixWhiteSpaces) {
        if (c.isWhitespace()) {
            if (!wasWhiteSpace) {
                append(c)
                wasWhiteSpace = true
            }
        } else {
            append(c)
            wasWhiteSpace = false
        }
    }
}.trimEnd()

fun List<Team>.getById(id: Long): Team? {
    return singleOrNull { it.id == id }
}
fun LongSparseArray<Team>.getById(id: Long): Team? {
    forEach { _, value ->
        if (value.id == id)
            return value
    }
    return null
}

operator fun MatchResult.get(group: Int): String {
    if (group >= groupValues.size)
        return ""
    return groupValues[group]
}