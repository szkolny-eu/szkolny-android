package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_NORMAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_YEAR_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_YEAR_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType

class MobidziennikHtmlGrades(
    override val data: DataMobidziennik,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikHtmlGrades"
    }

    init {
        if (data.profile == null)
            onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_GRADES)
        else
            sync(mutableListOf(1, 2))
    }

    private fun sync(semesters: MutableList<Int>) {
        if (semesters.isEmpty()) {
            syncFinal()
            return
        }
        val semester = semesters.removeAt(0)
        webGet(TAG, "/dziennik/oceny/?semestr=$semester") { text ->
            for (block in HtmlParsers.parseGrades(text, semester))
                for (grade in block.oceny)
                    addGrade(block.przedmiot, grade, semester)
            data.toRemove.add(DataRemoveModel.Grades.semesterWithType(semester, TYPE_NORMAL))
            sync(semesters)
        }
    }

    private fun syncFinal() {
        webGet(TAG, "/dziennik/oceny/?koncowe=true") { text ->
            for (g in HtmlParsers.parseFinalGrades(text))
                addFinalGrade(g)
            for ((semester, type) in listOf(
                1 to TYPE_SEMESTER1_PROPOSED, 1 to TYPE_SEMESTER1_FINAL,
                2 to TYPE_YEAR_PROPOSED, 2 to TYPE_YEAR_FINAL,
            ))
                data.toRemove.add(DataRemoveModel.Grades.semesterWithType(semester, type))
            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_HTML_GRADES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_GRADES)
        }
    }

    private fun finalType(header: String): Pair<Int, Int>? {
        val h = header.lowercase()
        val proposed = "przewid" in h
        return when {
            "średni" in h || "sredni" in h -> null
            "śródr" in h || "srodr" in h -> 1 to if (proposed) TYPE_SEMESTER1_PROPOSED else TYPE_SEMESTER1_FINAL
            "roczn" in h -> 2 to if (proposed) TYPE_YEAR_PROPOSED else TYPE_YEAR_FINAL
            else -> null
        }
    }

    private fun addFinalGrade(g: HtmlParsers.FinalGrade) {
        val (semester, type) = finalType(g.naglowek) ?: return
        val name = g.ocena.split(" ").first()
        val subject = data.htmlSubject(g.przedmiot)
        val id = htmlId("final", g.przedmiot, type.toString())
        data.gradeList.add(
            Grade(
                profileId = data.profileId,
                id = id,
                name = name,
                type = type,
                value = htmlGradeValue(name),
                weight = 0f,
                color = HtmlPlaceholder.GRADE_COLOR,
                category = null,
                description = null,
                comment = null,
                semester = semester,
                teacherId = -1,
                subjectId = subject?.id ?: -1,
            )
        )
        data.metadataList.add(
            Metadata(
                data.profileId,
                MetadataType.GRADE,
                id,
                data.profile?.empty ?: false,
                data.profile?.empty ?: false,
            )
        )
    }

    private fun addGrade(subjectName: String, g: HtmlParsers.Grade, semester: Int) {
        val subject = data.htmlSubject(subjectName)
        val teacher = data.htmlTeacher(g.nauczyciel)
        val id = g.id.toLongOrNull() ?: htmlId("grade", subjectName, g.ocena, g.wpisano, g.opis)
        val weight = if (g.liczonaDoSredniej) (g.waga ?: 0.0).toFloat() else 0f
        val description = if (g.liczonaDoSredniej) g.opis else "NLDŚR, ${g.opis}"

        val grade = Grade(
            profileId = data.profileId,
            id = id,
            name = g.ocena ?: "?",
            type = TYPE_NORMAL,
            value = (g.wartosc ?: 0.0).toFloat(),
            weight = weight,
            color = HtmlPlaceholder.GRADE_COLOR,
            category = g.kategoria,
            description = description,
            comment = null,
            semester = semester,
            teacherId = teacher?.id ?: -1,
            subjectId = subject?.id ?: -1,
            addedDate = data.htmlDateTimeMillis(g.wpisano) ?: System.currentTimeMillis(),
        )
        data.gradeList.add(grade)
        data.metadataList.add(
            Metadata(
                data.profileId,
                MetadataType.GRADE,
                id,
                data.profile?.empty ?: false,
                data.profile?.empty ?: false,
            )
        )
    }
}
