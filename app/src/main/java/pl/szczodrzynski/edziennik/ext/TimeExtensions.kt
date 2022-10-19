/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.Context
import im.wangchao.mhttp.Response
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import java.text.SimpleDateFormat
import java.util.Locale

const val MINUTE = 60L
const val HOUR = 60L*MINUTE
const val DAY = 24L*HOUR
const val WEEK = 7L*DAY
const val MONTH = 30L*DAY
const val YEAR = 365L*DAY
const val MS = 1000L

fun currentTimeUnix() = System.currentTimeMillis() / 1000

fun Response?.getUnixDate(): Long {
    val rfcDate = this?.headers()?.get("date") ?: return currentTimeUnix()
    val pattern = "EEE, dd MMM yyyy HH:mm:ss Z"
    val format = SimpleDateFormat(pattern, Locale.ENGLISH)
    return (format.parse(rfcDate)?.time ?: 0) / 1000
}

fun Long.formatDate(format: String = "yyyy-MM-dd HH:mm:ss"): String = SimpleDateFormat(format).format(this)

operator fun Time?.compareTo(other: Time?): Int {
    if (this == null && other == null)
        return 0
    if (this == null)
        return -1
    if (other == null)
        return 1
    return this.compareTo(other)
}

fun Context.timeTill(time: Int, delimiter: String = " ", countInSeconds: Boolean = false): String {
    val parts = mutableListOf<Pair<Int, Int>>()

    val hours = time / 3600
    val minutes = (time - hours*3600) / 60
    val seconds = time - minutes*60 - hours*3600

    if (!countInSeconds) {
        var prefixAdded = false
        if (hours > 0) {
            if (!prefixAdded) parts += R.plurals.time_till_text to hours
            prefixAdded = true
            parts += R.plurals.time_till_hours to hours
        }
        if (minutes > 0) {
            if (!prefixAdded) parts += R.plurals.time_till_text to minutes
            prefixAdded = true
            parts += R.plurals.time_till_minutes to minutes
        }
        if (hours == 0 && minutes < 10) {
            if (!prefixAdded) parts += R.plurals.time_till_text to seconds
            parts += R.plurals.time_till_seconds to seconds
        }
    } else {
        parts += R.plurals.time_till_text to time
        parts += R.plurals.time_till_seconds to time
    }

    return parts.joinToString(delimiter) { resources.getQuantityString(it.first, it.second, it.second) }
}

fun Context.timeLeft(time: Int, delimiter: String = " ", countInSeconds: Boolean = false): String {
    val parts = mutableListOf<Pair<Int, Int>>()

    val hours = time / 3600
    val minutes = (time - hours*3600) / 60
    val seconds = time - minutes*60 - hours*3600

    if (!countInSeconds) {
        var prefixAdded = false
        if (hours > 0) {
            if (!prefixAdded) parts += R.plurals.time_left_text to hours
            prefixAdded = true
            parts += R.plurals.time_left_hours to hours
        }
        if (minutes > 0) {
            if (!prefixAdded) parts += R.plurals.time_left_text to minutes
            prefixAdded = true
            parts += R.plurals.time_left_minutes to minutes
        }
        if (hours == 0 && minutes < 10) {
            if (!prefixAdded) parts += R.plurals.time_left_text to seconds
            parts += R.plurals.time_left_seconds to seconds
        }
    } else {
        parts += R.plurals.time_left_text to time
        parts += R.plurals.time_left_seconds to time
    }

    return parts.joinToString(delimiter) { resources.getQuantityString(it.first, it.second, it.second) }
}

fun Context.getSyncInterval(interval: Int): String {
    val hours = interval / 60 / 60
    val minutes = interval / 60 % 60
    val hoursText = if (hours > 0)
        plural(R.plurals.time_till_hours, hours)
    else
        null
    val minutesText = if (minutes > 0)
        plural(R.plurals.time_till_minutes, minutes)
    else
        ""
    return hoursText?.plus(" $minutesText") ?: minutesText
}

fun ClosedRange<Date>.asSequence(): Sequence<Date> = sequence {
    val date = this@asSequence.start.clone()
    while (date in this@asSequence) {
        yield(date.clone())
        date.stepForward(0, 0, 1)
    }
}
