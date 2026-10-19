package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.db.entity.Subject
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.entity.Team
import pl.szczodrzynski.edziennik.ext.crc32
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.fixName
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import timber.log.Timber

object HtmlPlaceholder {
    const val STUDENT_ID = 0

    const val CLASS_NAME = "Klasa"

    const val GRADE_COLOR = 0xff9e9e9e.toInt()

    fun studentName(data: DataMobidziennik) = data.loginUsername?.takeIf { it.isNotBlank() } ?: "Uczeń"
}

internal fun htmlId(vararg parts: String?): Long = parts.joinToString("|") { it ?: "" }.crc32()

internal fun DataMobidziennik.htmlYearFor(month: Int): Int {
    val schoolYearStart = profile?.dateSemester1Start?.year
        ?: Date.getToday().let { if (it.month < 9) it.year - 1 else it.year }
    return if (month >= 9) schoolYearStart else schoolYearStart + 1
}

internal fun DataMobidziennik.htmlDate(ymd: HtmlText.Ymd?): Date? {
    ymd ?: return null
    return try {
        Date(ymd.year ?: htmlYearFor(ymd.month), ymd.month, ymd.day)
    } catch (e: Exception) {
        null
    }
}

internal fun DataMobidziennik.htmlDate(text: String?, what: String): Date? {
    val date = htmlDate(HtmlText.parseDate(text))
    if (date == null)
        Timber.w("Html: no date in $what: '$text'")
    return date
}

internal fun htmlTime(hms: HtmlText.Hms?): Time? = hms?.let { Time(it.hour, it.minute, it.second) }

internal fun DataMobidziennik.htmlDateTimeMillis(text: String?): Long? {
    val date = htmlDate(HtmlText.parseDate(text)) ?: return null
    val time = htmlTime(HtmlText.parseTime(text)) ?: Time(0, 0, 0)
    return date.combineWith(time)
}

internal fun DataMobidziennik.htmlTeamClass(): Team {
    val name = profile?.studentClassName?.takeIf { it.isNotBlank() } ?: HtmlPlaceholder.CLASS_NAME
    val team = getTeam(null, name, loginServerName ?: "", isTeamClass = true)
    profile?.studentClassName = team.name
    return team
}

internal fun DataMobidziennik.htmlSetClassName(name: String?) {
    val clean = name?.trim()?.takeIf { it.isNotEmpty() } ?: return
    val team = htmlTeamClass()
    if (team.name == HtmlPlaceholder.CLASS_NAME || profile?.studentClassName == HtmlPlaceholder.CLASS_NAME) {
        team.name = clean
        team.code = "${loginServerName ?: ""}:$clean"
        profile?.studentClassName = clean
    }
}

internal fun DataMobidziennik.htmlSubject(name: String?): Subject? =
    name?.trim()?.takeIf { it.isNotEmpty() }?.let { getSubject(null, it) }

internal fun DataMobidziennik.htmlTeacher(name: String?): Teacher? =
    name?.takeIf { it.isNotBlank() }?.let { getTeacherByLastFirst(it.fixName()) }

internal class HtmlListDates {
    private val today = Date.getToday()
    private var year = today.year
    private var month = today.month

    fun date(text: String?): Date? {
        val ymd = HtmlText.parseDate(text) ?: return null
        return try {
            if (ymd.year != null) {
                year = ymd.year
                month = ymd.month
                Date(ymd.year, ymd.month, ymd.day)
            } else {
                if (ymd.month > month)
                    year--
                month = ymd.month
                Date(year, ymd.month, ymd.day)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun millis(text: String?): Long? {
        val date = date(text) ?: return null
        val time = htmlTime(HtmlText.parseTime(text)) ?: Time(0, 0, 0)
        return date.combineWith(time)
    }
}

internal data class HtmlAttachment(val id: Long, val name: String, val size: Long)

internal fun htmlAttachments(text: String, skipAnswers: Boolean = false): List<HtmlAttachment> =
    Regexes.MOBIDZIENNIK_WEB_ATTACHMENT.findAll(text).mapNotNull { match ->
        if (skipAnswers && match[1].isNotEmpty())
            return@mapNotNull null
        val id = match[2].toLongOrNull() ?: return@mapNotNull null
        var size = match[4].toFloatOrNull() ?: -1f
        when (match[5]) {
            "K" -> size *= 1024f
            "M" -> size *= 1024f * 1024f
            "G" -> size *= 1024f * 1024f * 1024f
        }
        HtmlAttachment(id, match[3], size.toLong())
    }.toList()

internal fun htmlGradeValue(name: String): Float {
    val m = Regex("^([1-6])([+-])?").find(name.trim()) ?: return 0f
    val base = m.groupValues[1].toFloat()
    return when (m.groupValues[2]) {
        "+" -> base + 0.5f
        "-" -> base - 0.25f
        else -> base
    }
}
