/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-25
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.librus.data.messages

import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.data.LibrusMessages

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
