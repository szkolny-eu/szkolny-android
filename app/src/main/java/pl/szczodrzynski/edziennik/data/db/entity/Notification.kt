/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.entity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.JsonObject
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.MainActivity

@Entity(tableName = "notifications")
data class Notification(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,

        val title: String,
        val text: String,
        val textLong: String? = null,

        val type: Int,

        val profileId: Int?,
        val profileName: String?,

        var posted: Boolean = true,

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
        const val TYPE_REMOVED_SHARED_EVENT = 18
        const val TYPE_NEW_MESSAGE = 8
        const val TYPE_NEW_NOTICE = 9
        const val TYPE_NEW_ATTENDANCE = 13
        const val TYPE_SERVER_MESSAGE = 11
        const val TYPE_LUCKY_NUMBER = 14
        const val TYPE_NEW_ANNOUNCEMENT = 15
        const val TYPE_FEEDBACK_MESSAGE = 16
        const val TYPE_AUTO_ARCHIVING = 17
        const val TYPE_TEACHER_ABSENCE = 19
        const val TYPE_NEW_SHARED_NOTE = 20

        fun buildId(profileId: Int, type: Int, itemId: Long): Long {
            return 1000000000000 + profileId*10000000000 + type*100000000 + itemId;
        }
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

    fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        fillIntent(intent)
        return PendingIntent.getActivity(context, id.toInt(), intent, PendingIntent.FLAG_ONE_SHOT)
    }

    fun getLargeIcon(): IIcon = when (type) {
        TYPE_TIMETABLE_LESSON_CHANGE -> CommunityMaterial.Icon3.cmd_timetable
        TYPE_NEW_GRADE -> CommunityMaterial.Icon3.cmd_numeric_5_box_outline
        TYPE_NEW_EVENT -> CommunityMaterial.Icon.cmd_calendar_outline
        TYPE_NEW_HOMEWORK -> CommunityMaterial.Icon3.cmd_notebook_outline
        TYPE_NEW_SHARED_EVENT -> CommunityMaterial.Icon.cmd_calendar_outline
        TYPE_NEW_SHARED_HOMEWORK -> CommunityMaterial.Icon3.cmd_notebook_outline
        TYPE_NEW_MESSAGE -> CommunityMaterial.Icon.cmd_email_outline
        TYPE_NEW_NOTICE -> CommunityMaterial.Icon.cmd_emoticon_outline
        TYPE_NEW_ATTENDANCE -> CommunityMaterial.Icon.cmd_calendar_remove_outline
        TYPE_LUCKY_NUMBER -> CommunityMaterial.Icon.cmd_emoticon_excited_outline
        TYPE_NEW_ANNOUNCEMENT -> CommunityMaterial.Icon.cmd_bullhorn_outline
        TYPE_NEW_SHARED_NOTE -> CommunityMaterial.Icon3.cmd_playlist_edit
        else -> CommunityMaterial.Icon.cmd_bell_ring_outline
    }
}
