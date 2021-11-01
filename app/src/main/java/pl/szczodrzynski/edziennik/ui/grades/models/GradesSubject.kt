/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-29.
 */

package pl.szczodrzynski.edziennik.ui.grades.models

import pl.szczodrzynski.edziennik.data.db.full.GradeFull

data class GradesSubject(
    val subjectId: Long,
    val subjectName: String,
    val semesters: MutableList<GradesSemester> = mutableListOf()
) : ExpandableItemModel<GradesSemester>(semesters) {
    override var level = 1

    var lastAddedDate = 0L
    var semester: Int = 1

    var hasUnseen: Boolean = false
        get() = field || semesters.any { it.hasUnseen }

    val averages = GradesAverages()
    var proposedGrade: GradeFull? = null
    var finalGrade: GradeFull? = null
}
