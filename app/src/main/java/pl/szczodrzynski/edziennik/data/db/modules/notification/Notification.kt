/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-18.
 */

package pl.szczodrzynski.edziennik.data.db.modules.notification

import android.content.Context
import android.content.Intent
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_AUTO_ARCHIVING
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_ERROR
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_FEEDBACK_MESSAGE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_GENERAL
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_ANNOUNCEMENT
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_ATTENDANCE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_EVENT
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_GRADE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_HOMEWORK
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_MESSAGE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_NOTICE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_NEW_SHARED_EVENT
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_SERVER_MESSAGE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_TIMETABLE_CHANGED
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_TIMETABLE_LESSON_CHANGE
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.Companion.TYPE_UPDATE

@Entity(tableName = "notifications")
data class Notification(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,

        val title: String,
        val text: String,

        val type: Int,

        val profileId: Int?,
        val profileName: String?,

        var posted: Boolean = false,

        var viewId: Int? = null,
        var extras: JsonObject? = null,

        val addedDate: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_GENERAL = 0
        const val TYPE_UPDATE = 1
        const val TYPE_ERROR = 2
        const val TYPE_TIMETABLE_CHANGED = 3
        const val TYPE_TIMETABLE_LESSON_CHANGE = 4
        const val TYPE_NEW_GRADE = 5
        const val TYPE_NEW_EVENT = 6
        const val TYPE_NEW_HOMEWORK = 10
        const val TYPE_NEW_SHARED_EVENT = 7
        const val TYPE_NEW_SHARED_HOMEWORK = 12
        const val TYPE_NEW_MESSAGE = 8
        const val TYPE_NEW_NOTICE = 9
        const val TYPE_NEW_ATTENDANCE = 13
        const val TYPE_SERVER_MESSAGE = 11
        const val TYPE_LUCKY_NUMBER = 14
        const val TYPE_NEW_ANNOUNCEMENT = 15
        const val TYPE_FEEDBACK_MESSAGE = 16
        const val TYPE_AUTO_ARCHIVING = 17
    }

    fun addExtra(key: String, value: Long?): Notification {
        extras = extras ?: JsonObject()
        extras?.addProperty(key, value)
        return this
    }
    fun addExtra(key: String, value: String?): Notification {
        extras = extras ?: JsonObject()
        extras?.addProperty(key, value)
        return this
    }

    fun fillIntent(intent: Intent) {
        if (profileId != -1)
            intent.putExtra("profileId", profileId)
        if (viewId != -1)
            intent.putExtra("fragmentId", viewId)
        try {
            extras?.entrySet()?.forEach { (key, value) ->
                if (!value.isJsonPrimitive)
                    return@forEach
                val primitive = value.asJsonPrimitive
                if (primitive.isNumber) {
                    intent.putExtra(key, primitive.asLong)
                } else if (primitive.isString) {
                    intent.putExtra(key, primitive.asString)
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }
}

fun Context.getNotificationTitle(type: Int): String {
    return getString(when (type) {
        TYPE_UPDATE -> R.string.notification_type_update
        TYPE_ERROR -> R.string.notification_type_error
        TYPE_TIMETABLE_CHANGED -> R.string.notification_type_timetable_change
        TYPE_TIMETABLE_LESSON_CHANGE -> R.string.notification_type_timetable_lesson_change
        TYPE_NEW_GRADE -> R.string.notification_type_new_grade
        TYPE_NEW_EVENT -> R.string.notification_type_new_event
        TYPE_NEW_HOMEWORK -> R.string.notification_type_new_homework
        TYPE_NEW_SHARED_EVENT -> R.string.notification_type_new_shared_event
        TYPE_NEW_MESSAGE -> R.string.notification_type_new_message
        TYPE_NEW_NOTICE -> R.string.notification_type_notice
        TYPE_NEW_ATTENDANCE -> R.string.notification_type_attendance
        TYPE_SERVER_MESSAGE -> R.string.notification_type_server_message
        TYPE_LUCKY_NUMBER -> R.string.notification_type_lucky_number
        TYPE_FEEDBACK_MESSAGE -> R.string.notification_type_feedback_message
        TYPE_NEW_ANNOUNCEMENT -> R.string.notification_type_new_announcement
        TYPE_AUTO_ARCHIVING -> R.string.notification_type_auto_archiving
        TYPE_GENERAL -> R.string.notification_type_general
        else -> R.string.notification_type_general
    })
}