/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-2.
 */

package pl.szczodrzynski.edziennik.api.v2.models

import pl.szczodrzynski.edziennik.utils.models.Date

class DataRemoveModel {
    var removeAll: Boolean? = null
    var removeSemester: Int? = null
    var removeDateFrom: Date? = null
    var removeDateTo: Date? = null

    constructor() {
        this.removeAll = true
    }

    constructor(semester: Int) {
        this.removeSemester = semester
    }

    constructor(dateFrom: Date?, dateTo: Date) {
        this.removeDateFrom = dateFrom
        this.removeDateTo = dateTo
    }

    constructor(dateFrom: Date) {
        this.removeDateFrom = dateFrom
    }
}