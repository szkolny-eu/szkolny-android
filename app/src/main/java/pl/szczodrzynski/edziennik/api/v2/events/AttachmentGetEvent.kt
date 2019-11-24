/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-24
 */

package pl.szczodrzynski.edziennik.api.v2.events

data class AttachmentGetEvent(val profileId: Int, val messageId: Long, val attachmentId: Long,
                              var eventType: Int = TYPE_PROGRESS, val fileName: String? = null,
                              val bytesWritten: Long = 0) {
    companion object {
        const val TYPE_PROGRESS = 0
        const val TYPE_FINISHED = 1
    }
}
