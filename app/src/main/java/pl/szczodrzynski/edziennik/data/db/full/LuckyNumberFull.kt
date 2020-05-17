/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.full

import pl.szczodrzynski.edziennik.data.db.entity.LuckyNumber
import pl.szczodrzynski.edziennik.utils.models.Date

class LuckyNumberFull(
        profileId: Int, date: Date,
        number: Int
) : LuckyNumber(
        profileId, date,
        number
) {
    // metadata
    var seen = false
    var notified = false
}
