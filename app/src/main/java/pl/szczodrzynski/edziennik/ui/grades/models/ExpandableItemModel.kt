/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-29.
 */

package pl.szczodrzynski.edziennik.ui.grades.models

import pl.szczodrzynski.edziennik.ui.grades.GradesAdapter.Companion.STATE_CLOSED

abstract class ExpandableItemModel<T>(open val items: MutableList<T>) {
    open var level: Int = 3
    var state: Int = STATE_CLOSED
}
