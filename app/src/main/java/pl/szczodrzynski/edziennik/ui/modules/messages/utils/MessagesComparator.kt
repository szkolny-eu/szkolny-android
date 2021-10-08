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
            // descending sorting (1. true, 2. false)
            o1.isStarred && !o2.isStarred -> -1
            !o1.isStarred && o2.isStarred -> 1
            // ascending sorting
            o1.filterWeight > o2.filterWeight -> 1
            o1.filterWeight < o2.filterWeight -> -1
            // descending sorting
            o1.addedDate > o2.addedDate -> -1
            o1.addedDate < o2.addedDate -> 1
            else -> 0
        }
    }
}
