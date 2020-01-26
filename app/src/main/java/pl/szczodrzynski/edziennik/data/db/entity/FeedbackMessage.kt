/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-21.
 */

package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feedbackMessages")
open class FeedbackMessage(
        @PrimaryKey(autoGenerate = true)
        val messageId: Int = 0,

        val received: Boolean,
        val text: String,

        // used always - contains the sender name
        val senderName: String,

        // used in DEV apps - contains device ID and model
        var deviceId: String? = null,
        var deviceName: String? = null,

        val devId: Int? = null,
        val devImage: String? = null,

        val sentTime: Long = System.currentTimeMillis()
) {
        class WithCount(messageId: Int, received: Boolean, text: String, senderName: String, deviceId: String?, deviceName: String?, devId: Int?, devImage: String?, sentTime: Long) : FeedbackMessage(messageId, received, text, senderName, deviceId, deviceName, devId, devImage, sentTime) {
                var count = 0
        }
}
