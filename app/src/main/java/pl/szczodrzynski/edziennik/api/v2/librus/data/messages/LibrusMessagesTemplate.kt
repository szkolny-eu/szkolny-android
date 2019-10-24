/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-25
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.messages

import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS

class LibrusMessagesTemplate(override val data: DataLibrus, val onSuccess: () -> Unit) : LibrusMessages(data) {
    companion object {
        const val TAG = "LibrusMessages"
    }

    init {
        /* messagesGet(TAG, "") { doc ->

            data.setSyncNext(ENDPOINT_LIBRUS_MESSAGES_, SYNC_ALWAYS)
            onSuccess()
        } */
    }
}
