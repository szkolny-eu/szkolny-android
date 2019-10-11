/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-10.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web

import android.graphics.Color
import com.google.gson.JsonParser
import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.api.v2.ENDPOINT_LIBRUS_API_ME
import pl.szczodrzynski.edziennik.api.v2.Regexes
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_GRADES
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.fixWhiteSpaces
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.Utils.strToInt
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import java.util.*
import java.util.regex.Pattern

class MobidziennikWebGrades(override val data: DataMobidziennik,
                              val onSuccess: () -> Unit) : MobidziennikWeb(data)  {
    companion object {
        private const val TAG = "MobidziennikWebGrades"
    }

    init {
        webGet(TAG, "/dziennik/oceny?semestr=${profile?.currentSemester ?: 1}") { text ->
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
                            subjectName = it[1]
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
                            gradeSemester = profile?.dateToSemester(gradeAddedDate) ?: 1
                        }

                        if (Regexes.MOBIDZIENNIK_GRADES_COUNT_TO_AVG.containsMatchIn(html)) {
                            Regexes.MOBIDZIENNIK_GRADES_DETAILS.find(html)?.let { match ->
                                val gradeName = match[1]
                                val gradeDescription = match[2]
                                val gradeValue = match[3].toFloatOrNull() ?: 0.0f
                                val teacherName = match[4].fixWhiteSpaces()

                                val teacherId = data.teacherList.singleOrNull { it.fullNameLastFirst == teacherName }?.id ?: -1
                                val subjectId = data.subjectList.singleOrNull { it.longName == subjectName }?.id ?: -1

                                val gradeObject = Grade(
                                        profileId,
                                        gradeId,
                                        gradeCategory,
                                        gradeColor,
                                        "NLDŚR, $gradeDescription",
                                        gradeName,
                                        gradeValue,
                                        0f,
                                        gradeSemester,
                                        teacherId,
                                        subjectId
                                )

                                gradeObject.classAverage = gradeClassAverage

                                data.gradeList.add(gradeObject)
                                data.metadataList.add(
                                        Metadata(
                                                profileId,
                                                Metadata.TYPE_GRADE,
                                                gradeObject.id,
                                                profile?.empty ?: false,
                                                profile?.empty ?: false,
                                                gradeAddedDateMillis
                                        ))
                            }
                        } else {
                            data.gradeAverages.put(gradeId, gradeClassAverage)
                            data.gradeAddedDates.put(gradeId, gradeAddedDateMillis)
                            data.gradeColors.put(gradeId, gradeColor)
                        }
                    }
                }
            }

            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_GRADES, SYNC_ALWAYS)
            onSuccess()
        }
    }
}