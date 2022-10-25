/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-10.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import android.graphics.Color
import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_NORMAL
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.dateToSemester
import pl.szczodrzynski.edziennik.ext.fixWhiteSpaces
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class MobidziennikWebGrades(override val data: DataMobidziennik,
                            override val lastSync: Long?,
                            val onSuccess: (endpointId: Int) -> Unit
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikWebGrades"
    }

    init { data.profile?.also { profile ->
        val currentSemester = profile.currentSemester

        webGet(TAG, "/dziennik/oceny?semestr=$currentSemester") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            val doc = Jsoup.parse(text)

            val grades = doc.select("table.spis a, table.spis span, table.spis div")

            var gradeCategory = ""
            var gradeColor = -1
            var subjectName = ""

            for (e in grades) {
                when (e.tagName()) {
                    "div" -> {
                        Regexes.MOBIDZIENNIK_GRADES_SUBJECT_NAME.find(e.outerHtml())?.let {
                            subjectName = it[1].trim()
                        }
                    }
                    "span" -> {
                        val css = e.attr("style")
                        Regexes.MOBIDZIENNIK_GRADES_COLOR.find(css)?.let {
                            // (#2196f3)
                            gradeColor = Color.parseColor(it[1])
                        }
                        Regexes.MOBIDZIENNIK_GRADES_CATEGORY.find(e.outerHtml())?.let {
                            // (category)
                            gradeCategory = it[1]
                        }
                    }
                    "a" -> {
                        val gradeId = e.attr("rel").toLong()
                        var gradeAddedDateMillis: Long = -1
                        var gradeSemester = 1

                        val html = e.html()
                        val gradeClassAverage = Regexes.MOBIDZIENNIK_GRADES_CLASS_AVERAGE.find(html)?.let {
                            // (4.75)
                            it[1].toFloatOrNull()
                        } ?: -1f

                        Regexes.MOBIDZIENNIK_GRADES_ADDED_DATE.find(html)?.let {
                            // (2) (stycznia) (2019), (12:34:56)
                            val month = when (it[2]) {
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
                                else -> 1
                            }
                            val gradeAddedDate = Date(
                                    it[3].toInt(),
                                    month,
                                    it[1].toInt()
                            )
                            val time = Time.fromH_m_s(it[4])
                            gradeAddedDateMillis = gradeAddedDate.combineWith(time)
                            gradeSemester = profile.dateToSemester(gradeAddedDate)
                        }

                        if (Regexes.MOBIDZIENNIK_GRADES_COUNT_TO_AVG.containsMatchIn(html)) {
                            Regexes.MOBIDZIENNIK_GRADES_DETAILS.find(html)?.let { match ->
                                val gradeName = match[1]
                                var gradeDescription = match[2]
                                val gradeValue = match[3].toFloatOrNull() ?: 0.0f
                                val teacherName = match[4].fixWhiteSpaces()

                                val teacherId = data.teacherList.singleOrNull { it.fullNameLastFirst == teacherName }?.id ?: -1
                                val subjectId = data.subjectList.singleOrNull { it.longName == subjectName }?.id ?: -1

                                if (match[5].isNotEmpty()) {
                                    gradeDescription += "\n"+match[5].replace("<br>", "\n")
                                }

                                val gradeObject = Grade(
                                        profileId = profileId,
                                        id = gradeId,
                                        name = gradeName,
                                        type = TYPE_NORMAL,
                                        value = gradeValue,
                                        weight = 0f,
                                        color = gradeColor,
                                        category = gradeCategory,
                                        description = "NLDŚR, $gradeDescription",
                                        comment = null,
                                        semester = gradeSemester,
                                        teacherId = teacherId,
                                        subjectId = subjectId,
                                        addedDate = gradeAddedDateMillis
                                )

                                gradeObject.classAverage = gradeClassAverage

                                data.gradeList.add(gradeObject)
                                data.metadataList.add(
                                        Metadata(
                                                profileId,
                                                MetadataType.GRADE,
                                                gradeObject.id,
                                                profile.empty,
                                                profile.empty
                                        ))
                            }
                        }
                        data.gradeAverages[gradeId] = gradeClassAverage
                        data.gradeAddedDates[gradeId] = gradeAddedDateMillis
                        data.gradeColors[gradeId] = gradeColor
                    }
                }
            }

            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_GRADES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_GRADES)
        }
    }}
}
