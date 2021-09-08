/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-14.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages.utils

import pl.szczodrzynski.edziennik.data.db.full.MessageFull

class MessagesComparator : Comparator<Any> {

    override fun compare(o1: Any?, o2: Any?): Int {
        if (o1 !is MessageFull || o2 !is MessageFull)
            return 0

        return when {
            // standard sorting
            o1.filterWeight > o2.filterWeight -> 1
            o1.filterWeight < o2.filterWeight -> -1
            else -> when {
                // reversed sorting
                o1.addedDate > o2.addedDate -> -1
                o1.addedDate < o2.addedDate -> 1
                else -> 0
            }
        }
    }
}
