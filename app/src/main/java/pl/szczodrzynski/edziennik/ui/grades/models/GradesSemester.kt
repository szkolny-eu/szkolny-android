/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-29.
 */

package pl.szczodrzynski.edziennik.ui.grades.models

import pl.szczodrzynski.edziennik.data.db.full.GradeFull

data class GradesSemester(
    val subjectId: Long,
    val number: Int,
    val grades: MutableList<GradeFull> = mutableListOf()
) : ExpandableItemModel<GradeFull>(grades) {
    override var level = 2

    var hasUnseen = false

    val averages = GradesAverages()
    var proposedGrade: GradeFull? = null
    var finalGrade: GradeFull? = null
}
