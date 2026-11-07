package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import pl.szczodrzynski.edziennik.data.api.POST
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_ABSENT
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_ABSENT_EXCUSED
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_BELATED
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_BELATED_EXCUSED
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_PRESENT
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_PRESENT_CUSTOM
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_RELEASED
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_UNKNOWN
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.dateToSemester
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import timber.log.Timber

class MobidziennikHtmlAttendance(
    override val data: DataMobidziennik,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikHtmlAttendance"

        private val STANDARD_SYMBOLS = setOf(".", "|", "+", "s", "z")

        private const val COLOR_TRUANCY = 0xff9c27b0.toInt()

        fun baseType(symbol: String, name: String): Int {
            val type = baseTypeByName(symbol, name)
            return if (type == TYPE_PRESENT && symbol.isNotEmpty() && symbol !in STANDARD_SYMBOLS)
                TYPE_PRESENT_CUSTOM
            else
                type
        }

        private fun baseTypeByName(symbol: String, name: String): Int {
            val n = name.lowercase()
            return when {
                "ucieczk" in n -> TYPE_ABSENT
                "nieusprawiedl" in n -> TYPE_ABSENT
                "spóźni" in n || "spozni" in n ->
                    if ("usprawiedl" in n) TYPE_BELATED_EXCUSED else TYPE_BELATED
                "usprawiedl" in n -> TYPE_ABSENT_EXCUSED
                "zwoln" in n -> TYPE_RELEASED
                "nieobecn" in n -> TYPE_ABSENT
                "obecn" in n -> TYPE_PRESENT
                else -> when (symbol) {
                    "." -> TYPE_PRESENT
                    "|" -> TYPE_ABSENT
                    "+" -> TYPE_ABSENT_EXCUSED
                    "s" -> TYPE_BELATED
                    "z" -> TYPE_RELEASED
                    "e", "ep" -> TYPE_PRESENT_CUSTOM
                    "en" -> TYPE_ABSENT
                    "+ₑ" -> TYPE_ABSENT_EXCUSED
                    else -> TYPE_UNKNOWN
                }
            }
        }

        fun typeColor(symbol: String, name: String): Int? {
            val custom = when (symbol) {
                "e" -> 0xff673ab7
                "en" -> 0xffec407a
                "ep" -> 0xff4caf50
                "+ₑ" -> 0xff795548
                else -> null
            }?.toInt()
            return custom ?: if ("ucieczk" in name.lowercase()) COLOR_TRUANCY else null
        }
    }

    init {
        val profile = data.profile
        if (profile == null) {
            onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_ATTENDANCE)
        } else if (data.studentId == HtmlPlaceholder.STUDENT_ID) {
            Timber.w("Html: no student id yet, the attendance page needs it - skipping")
            onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_ATTENDANCE)
        } else {
            val syncFrom = lastSync?.let { Date.fromMillis(it) } ?: profile.dateSemester1Start
            var weekStart = Week.getWeekStart()
            val syncWeeks = mutableListOf(weekStart)
            while (weekStart >= syncFrom && weekStart > profile.dateSemester1Start) {
                weekStart = weekStart.clone().stepForward(0, 0, -7)
                syncWeeks += weekStart
            }
            syncWeeks.minByOrNull { it.value }?.let {
                data.toRemove.add(DataRemoveModel.Attendance.from(it))
            }
            start(syncWeeks)
        }
    }

    private fun start(syncWeeks: MutableList<Date>) {
        if (syncWeeks.isEmpty()) {
            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_HTML_ATTENDANCE, SYNC_ALWAYS)
            onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_ATTENDANCE)
            return
        }
        sync(syncWeeks.removeAt(0).stringY_m_d) {
            start(syncWeeks)
        }
    }

    private fun sync(weekStart: String, onSuccess: () -> Unit) {
        webGet(TAG, "/dziennik/frekwencja", method = POST, parameters = listOf(
            "uczen" to data.studentId,
            "data_poniedzialek" to weekStart,
        )) { text ->
            val types = Regexes.MOBIDZIENNIK_ATTENDANCE_TYPES
                .find(text)
                ?.get(1)
                ?.split("<br/>")
                ?.map {
                    it.trimEnd(',')
                        .split(" ", limit = 2)
                        .let { parts -> parts.getOrNull(0) to parts.getOrNull(1) }
                }
                ?.toMap()
            val typeSymbols = types?.keys?.filterNotNull() ?: listOf()

            Regexes.MOBIDZIENNIK_ATTENDANCE_TABLE.findAll(text).forEach { tableResult ->
                val table = tableResult[1]

                val lessonDates = mutableListOf<Date>()
                val entries = mutableListOf<String>()
                val ranges = mutableListOf<MatchResult?>()

                Regexes.MOBIDZIENNIK_ATTENDANCE_LESSON_COUNT.findAll(table).forEach {
                    val date = Date.fromY_m_d(it[1])
                    for (i in 0 until (it[2].toIntOrNull() ?: 0)) {
                        lessonDates += date
                    }
                }
                Regexes.MOBIDZIENNIK_ATTENDANCE_ENTRIES.findAll(table).mapTo(entries) { it[1] }

                Regexes.MOBIDZIENNIK_ATTENDANCE_COLUMNS.findAll(table).forEach { columns ->
                    var index = 0
                    Regexes.MOBIDZIENNIK_ATTENDANCE_COLUMN.findAll(columns[1]).forEach { column ->
                        if (column[1].contains("colspan")) {
                            val colspan = Regexes.MOBIDZIENNIK_ATTENDANCE_COLUMN_SPAN.find(column[1])
                                ?.get(1)
                                ?.toIntOrNull() ?: 0
                            entries.addAll(index, List(colspan) { "" })
                            ranges.addAll(List(colspan) { null })
                            index += colspan
                        } else {
                            ranges.add(Regexes.MOBIDZIENNIK_ATTENDANCE_RANGE.find(column[2]))
                            index++
                        }
                    }
                }

                val dateIterator = lessonDates.iterator()
                val entriesIterator = entries.iterator()

                val count = ranges.count()
                if (count != lessonDates.count() || count != entries.count())
                    return@forEach
                ranges.onEach { range ->
                    val lessonDate = dateIterator.next()
                    val entry = entriesIterator.next()
                    if (range == null || entry.isBlank())
                        return@onEach
                    val startTime = Time.fromH_m(range[1])

                    range[2].split(" / ").mapNotNull {
                        Regexes.MOBIDZIENNIK_ATTENDANCE_LESSON.find(it)
                    }.forEachIndexed { index, lesson ->
                        processEntry(index, lesson, lessonDate, startTime, entry, types, typeSymbols)
                    }
                }
            }

            onSuccess()
        }
    }

    private fun processEntry(
        index: Int,
        lesson: MatchResult,
        lessonDate: Date,
        startTime: Time,
        entry: String,
        types: Map<String?, String?>?,
        typeSymbols: List<String>,
    ) {
        val profile = data.profile ?: return
        val topic = lesson[1].substringAfter(" - ", missingDelimiterValue = "").takeIf { it.isNotBlank() }
        if (topic?.startsWith("Lekcja odwołana: ") == true || entry.isEmpty())
            return
        val subjectName = lesson[1].substringBefore(" - ").trim()

        var typeSymbol = ""
        for (symbol in typeSymbols) {
            if (entry.startsWith(symbol) && symbol.length > typeSymbol.length)
                typeSymbol = symbol
        }

        val typeName = types?.get(typeSymbol) ?: ""
        val baseType = baseType(typeSymbol, typeName)
        val isCustom = typeSymbol !in STANDARD_SYMBOLS
        val typeShort = if (!isCustom)
            data.app.attendanceManager.getTypeShort(baseType)
        else
            typeSymbol

        val id = lessonDate.combineWith(startTime) / 6L * 10L + (lesson[0].hashCode() and 0xFFFF) + index

        val attendance = Attendance(
            profileId = data.profileId,
            id = id,
            baseType = baseType,
            typeName = typeName,
            typeShort = typeShort,
            typeSymbol = typeSymbol,
            typeColor = typeColor(typeSymbol, typeName),
            date = lessonDate,
            startTime = startTime,
            semester = profile.dateToSemester(lessonDate),
            teacherId = data.htmlTeacher(lesson[3])?.id ?: -1,
            subjectId = data.htmlSubject(subjectName)?.id ?: -1,
        ).also {
            it.lessonTopic = topic
        }

        data.attendanceList.add(attendance)
        if (baseType != TYPE_PRESENT) {
            val seen = profile.empty || baseType == TYPE_PRESENT_CUSTOM || baseType == TYPE_UNKNOWN
            data.metadataList.add(
                Metadata(data.profileId, MetadataType.ATTENDANCE, id, seen, seen)
            )
        }
    }
}
