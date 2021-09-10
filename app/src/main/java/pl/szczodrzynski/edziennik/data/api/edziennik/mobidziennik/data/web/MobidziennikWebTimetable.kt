/*
 * Copyright (c) Kuba Szczodrzyński 2021-9-8.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import android.annotation.SuppressLint
import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_TIMETABLE
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import kotlin.collections.set
import kotlin.text.replace

class MobidziennikWebTimetable(
    override val data: DataMobidziennik,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikWebTimetable"
    }

    private val rangesH = mutableMapOf<ClosedFloatingPointRange<Float>, Date>()
    private val hoursV = mutableMapOf<Int, Pair<Time, Int?>>()
    private var startDate: Date

    private fun parseCss(css: String): Map<String, String> {
        return css.split(";").mapNotNull {
            val spl = it.split(":")
            if (spl.size != 2)
                return@mapNotNull null
            return@mapNotNull spl[0].trim() to spl[1].trim()
        }.toMap()
    }

    private fun getRangeH(h: Float): Date? {
        return rangesH.entries.firstOrNull {
            h in it.key
        }?.value
    }

    private fun stringToDate(date: String): Date? {
        val items = date.split(" ")
        val day = items.getOrNull(0)?.toIntOrNull() ?: return null
        val year = items.getOrNull(2)?.toIntOrNull() ?: return null
        val month = when (items.getOrNull(1)) {
            "stycznia" -> 1
            "lutego" -> 2
            "marca" -> 3
            "kwietnia" -> 4
            "maja" -> 5
            "czerwca" -> 6
            "lipca" -> 7
            "sierpnia" -> 8
            "września" -> 9
            "października" -> 10
            "listopada" -> 11
            "grudnia" -> 12
            else -> return null
        }
        return Date(year, month, day)
    }

    init {
        val currentWeekStart = Week.getWeekStart()
        val nextWeekEnd = Week.getWeekEnd().stepForward(0, 0, 7)
        if (Date.getToday().weekDay > 4) {
            currentWeekStart.stepForward(0, 0, 7)
        }
        startDate = data.arguments?.getString("weekStart")?.let {
            Date.fromY_m_d(it)
        } ?: currentWeekStart

        val syncFutureDate = startDate > nextWeekEnd
        // TODO: 2021-09-09 make DataRemoveModel keep extra lessons
        val syncExtraLessons = false && System.currentTimeMillis() - (lastSync ?: 0) > 2 * DAY * MS
        if (!syncFutureDate && !syncExtraLessons) {
            onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_TIMETABLE)
        }
        else {
            val types = when {
                syncFutureDate -> mutableListOf("podstawowy")//, "pozalekcyjny")
                syncExtraLessons -> mutableListOf("pozalekcyjny")
                else -> mutableListOf()
            }

            syncTypes(types, startDate) {
                // set as synced now only when not syncing future date
                // (to avoid waiting 2 days for normal sync after future sync)
                if (syncExtraLessons)
                    data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_TIMETABLE, SYNC_ALWAYS)
                onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_TIMETABLE)
            }
        }
    }

    private fun syncTypes(types: MutableList<String>, startDate: Date, onSuccess: () -> Unit) {
        if (types.isEmpty()) {
            onSuccess()
            return
        }
        val type = types.removeAt(0)
        webGet(TAG, "/dziennik/planlekcji?typ=$type&tydzien=${startDate.stringY_m_d}") { html ->
            MobidziennikLuckyNumberExtractor(data, html)
            readRangesH(html)
            readRangesV(html)
            readLessons(html)
            syncTypes(types, startDate, onSuccess)
        }
    }

    private fun readRangesH(html: String) {
        val htmlH = Regexes.MOBIDZIENNIK_TIMETABLE_TOP.find(html) ?: return
        val docH = Jsoup.parse(htmlH.value)

        var posH = 0f
        for (el in docH.select("div > div")) {
            val css = parseCss(el.attr("style"))
            val width = css["width"]
                ?.trimEnd('%')
                ?.toFloatOrNull()
                ?: continue
            val value = stringToDate(el.attr("title"))
                ?: continue

            val range = posH.rangeTo(posH + width)
            posH += width

            rangesH[range] = value
        }
    }

    private fun readRangesV(html: String) {
        val htmlV = Regexes.MOBIDZIENNIK_TIMETABLE_LEFT.find(html) ?: return
        val docV = Jsoup.parse(htmlV.value)

        for (el in docV.select("div > div")) {
            val css = parseCss(el.attr("style"))
            val top = css["top"]
                ?.trimEnd('%')
                ?.toFloatOrNull()
                ?: continue
            val values = el.text().split(" ")

            val time = values.getOrNull(0)?.let {
                Time.fromH_m(it)
            } ?: continue
            val num = values.getOrNull(1)?.toIntOrNull()

            hoursV[(top * 100).toInt()] = time to num
        }
    }

    private val whitespaceRegex = "\\s+".toRegex()
    private val classroomRegex = "\\((.*)\\)".toRegex()
    private fun cleanup(str: String): List<String> {
        return str
            .replace(whitespaceRegex, " ")
            .replace("\n", "")
            .replace("&lt;small&gt;", "$")
            .replace("&lt;/small&gt;", "$")
            .replace("&lt;br /&gt;", "\n")
            .replace("&lt;br/&gt;", "\n")
            .replace("&lt;br&gt;", "\n")
            .replace("<br />", "\n")
            .replace("<br/>", "\n")
            .replace("<br>", "\n")
            .replace("<b>", "%")
            .replace("</b>", "%")
            .replace("<span>", "")
            .replace("</span>", "")
            .split("\n")
            .map { it.trim() }
    }

    @SuppressLint("LongLogTag", "LogNotTimber")
    private fun readLessons(html: String) {
        val matches = Regexes.MOBIDZIENNIK_TIMETABLE_CELL.findAll(html)

        val noLessonDays = mutableListOf<Date>()
        for (i in 0..6) {
            noLessonDays.add(startDate.clone().stepForward(0, 0, i))
        }

        for (match in matches) {
            val css = parseCss("${match[1]};${match[2]}")
            val left = css["left"]?.trimEnd('%')?.toFloatOrNull() ?: continue
            val top = css["top"]?.trimEnd('%')?.toFloatOrNull() ?: continue
            val width = css["width"]?.trimEnd('%')?.toFloatOrNull() ?: continue
            val height = css["height"]?.trimEnd('%')?.toFloatOrNull() ?: continue

            val posH = left + width / 2f
            val topInt = (top * 100).toInt()
            val bottomInt = ((top + height) * 100).toInt()

            val lessonDate = getRangeH(posH) ?: continue
            val (startTime, lessonNumber) = hoursV[topInt] ?: continue
            val endTime = hoursV[bottomInt]?.first ?: continue

            noLessonDays.remove(lessonDate)

            var typeName: String? = null
            var subjectName: String? = null
            var teacherName: String? = null
            var classroomName: String? = null
            var teamName: String? = null
            val items = (cleanup(match[3]) + cleanup(match[4])).toMutableList()

            var length = 0
            while (items.isNotEmpty() && length != items.size) {
                length = items.size
                val toRemove = mutableListOf<String?>()
                items.forEachIndexed { i, item ->
                    when {
                        item.isEmpty() ->
                            toRemove.add(item)
                        item.contains(":") && item.contains(" - ") ->
                            toRemove.add(item)

                        item.startsWith("%") -> {
                            subjectName = item.trim('%')
                            // I have no idea what's going on here
                            // ok now seriously.. the subject (long or short) item
                            // may NOT be 0th, as the HH:MM - HH:MM item may be before
                            // or even the typeName item. As these are always **before**,
                            // they are removed in previous iterations, so the first not removed
                            // item should be the long/short subjectName needing to be removed now.
                            toRemove.add(items[toRemove.size])
                            // ...and this has to be added later
                            toRemove.add(item)
                        }

                        item.startsWith("&") -> {
                            typeName = item.trim('&')
                            toRemove.add(item)
                        }
                        typeName != null && (item.contains(typeName!!) || item.contains("</small>")) -> {
                            toRemove.add(item)
                        }

                        item.contains("(") && item.contains(")") -> {
                            classroomName = classroomRegex.find(item)?.get(1)
                            items[i] = item.replace("($classroomName)", "").trim()
                        }
                        classroomName != null && item.contains(classroomName!!) -> {
                            items[i] = item.replace("($classroomName)", "").trim()
                        }

                        item.contains("class=\"wyjatek tooltip\"") ->
                            toRemove.add(item)
                    }
                }
                items.removeAll(toRemove)
            }

            if (items.size == 2 && items[0].contains(" - ")) {
                val parts = items[0].split(" - ")
                teamName = parts[0]
                teacherName = parts[1]
            }
            else if (items.size == 2 && typeName?.contains("odwołana") == true) {
                teamName = items[0]
            }
            else if (items.size == 4) {
                teamName = items[0]
                teacherName = items[1]
            }

            val type = when (typeName) {
                "zastępstwo" -> Lesson.TYPE_CHANGE
                "lekcja odwołana", "odwołana" -> Lesson.TYPE_CANCELLED
                else -> Lesson.TYPE_NORMAL
            }
            val subject = subjectName?.let { data.getSubject(null, it) }
            val teacher = teacherName?.let { data.getTeacherByLastFirst(it) }
            val team = teamName?.let { data.getTeam(
                id = null,
                name = it,
                schoolCode = data.loginServerName ?: return@let null,
                isTeamClass = false
            ) }

            Lesson(data.profileId, -1).also {
                it.type = type
                if (type == Lesson.TYPE_CANCELLED) {
                    it.oldDate = lessonDate
                    it.oldLessonNumber = lessonNumber
                    it.oldStartTime = startTime
                    it.oldEndTime = endTime
                    it.oldSubjectId = subject?.id ?: -1
                    it.oldTeamId = team?.id ?: -1
                }
                else {
                    it.date = lessonDate
                    it.lessonNumber = lessonNumber
                    it.startTime = startTime
                    it.endTime = endTime
                    it.subjectId = subject?.id ?: -1
                    it.teacherId = teacher?.id ?: -1
                    it.teamId = team?.id ?: -1
                    it.classroom = classroomName
                }

                it.id = it.buildId()

                val seen = profile?.empty == false || lessonDate < Date.getToday()

                if (it.type != Lesson.TYPE_NORMAL) {
                    data.metadataList.add(
                        Metadata(
                            data.profileId,
                            Metadata.TYPE_LESSON_CHANGE,
                            it.id,
                            seen,
                            seen
                        )
                    )
                }
                data.lessonList += it
            }
        }

        for (date in noLessonDays) {
            data.lessonList += Lesson(data.profileId, date.value.toLong()).also {
                it.type = Lesson.TYPE_NO_LESSONS
                it.date = date
            }
        }
    }
}

