/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.entity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.JsonObject
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.enums.NotificationType
import pl.szczodrzynski.edziennik.ext.pendingIntentFlag
import pl.szczodrzynski.edziennik.ext.putExtras
import pl.szczodrzynski.edziennik.data.enums.NavTarget

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true)
        val id: Long = 0,

    val title: String,
    val text: String,
    val textLong: String? = null,

    val type: NotificationType,

    val profileId: Int?,
    val profileName: String?,

    var posted: Boolean = true,

    @ColumnInfo(name = "viewId")
        var navTarget: NavTarget? = null,
    var extras: JsonObject? = null,

    val addedDate: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(profileId: Int, type: NotificationType, itemId: Long): Long {
            return 1000000000000 + profileId*10000000000 + type.id*100000000 + itemId;
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
        if (navTarget != null)
            intent.putExtras("fragmentId" to navTarget)
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
        return PendingIntent.getActivity(context, id.toInt(), intent, PendingIntent.FLAG_ONE_SHOT or pendingIntentFlag())
    }
}
