/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-22.
 */

package pl.szczodrzynski.edziennik.data.api.events

import pl.szczodrzynski.edziennik.data.db.entity.Teacher

data class RecipientListGetEvent(val profileId: Int, val teacherList: List<Teacher>)
