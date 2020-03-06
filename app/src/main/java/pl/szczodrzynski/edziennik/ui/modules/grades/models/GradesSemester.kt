/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-29.
 */

package pl.szczodrzynski.edziennik.ui.modules.grades.models

import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.full.GradeFull

data class GradesSemester(
    val subjectId: Long,
    val number: Int,
    val grades: MutableList<Grade> = mutableListOf()
) : ExpandableItemModel<Grade>(grades) {
    override var level = 2

    val averages = GradesAverages()
    var proposedGrade: GradeFull? = null
    var finalGrade: GradeFull? = null
}
