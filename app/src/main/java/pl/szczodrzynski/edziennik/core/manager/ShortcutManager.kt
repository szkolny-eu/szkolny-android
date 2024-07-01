/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-1. 
 */

package pl.szczodrzynski.edziennik.core.manager

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.getSystemService
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.ext.putExtras

class ShortcutManager(val app: App) {

    data class Shortcut(
        val id: String,
        val label: Int,
        val icon: Int,
        val target: NavTarget,
    )

    private val shortcuts = listOf(
        Shortcut(
            id = "item_timetable",
            label = R.string.shortcut_timetable,
            icon = R.mipmap.ic_shortcut_timetable,
            target = NavTarget.TIMETABLE,
        ),
        Shortcut(
            id = "item_agenda",
            label = R.string.shortcut_agenda,
            icon = R.mipmap.ic_shortcut_agenda,
            target = NavTarget.AGENDA,
        ),
        Shortcut(
            id = "item_grades",
            label = R.string.shortcut_grades,
            icon = R.mipmap.ic_shortcut_grades,
            target = NavTarget.GRADES,
        ),
        Shortcut(
            id = "item_homeworks",
            label = R.string.shortcut_homework,
            icon = R.mipmap.ic_shortcut_homework,
            target = NavTarget.HOMEWORK,
        ),
        Shortcut(
            id = "item_messages",
            label = R.string.shortcut_messages,
            icon = R.mipmap.ic_shortcut_messages,
            target = NavTarget.MESSAGES,
        ),
    )

    fun createShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            app.getSystemService<ShortcutManager>()?.dynamicShortcuts = shortcuts.map {
                ShortcutInfo.Builder(app, it.id)
                    .setShortLabel(app.getString(it.label))
                    .setLongLabel(app.getString(it.label))
                    .setIcon(Icon.createWithResource(app, it.icon))
                    .setIntent(
                        Intent(Intent.ACTION_MAIN, null, app, MainActivity::class.java)
                            .putExtras("fragmentId" to it.target)
                    )
                    .build()
            }
        }
    }
}
