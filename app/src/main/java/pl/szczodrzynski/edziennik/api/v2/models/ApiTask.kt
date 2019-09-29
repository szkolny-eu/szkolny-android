/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.api.v2.models

open class ApiTask(open val profileId: Int) {
    var taskId: Int = 0
}