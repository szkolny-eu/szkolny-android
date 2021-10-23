/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-3.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.widget.TextView
import androidx.core.view.isVisible
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.mikepenz.iconics.view.IconicsTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ext.join
import pl.szczodrzynski.edziennik.ext.resolveColor
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import kotlin.coroutines.CoroutineContext

class EventManager(val app: App) : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    /*    _    _ _____    _____                 _  __ _
         | |  | |_   _|  / ____|               (_)/ _(_)
         | |  | | | |   | (___  _ __   ___  ___ _| |_ _  ___
         | |  | | | |    \___ \| '_ \ / _ \/ __| |  _| |/ __|
         | |__| |_| |_   ____) | |_) |  __/ (__| | | | | (__
          \____/|_____| |_____/| .__/ \___|\___|_|_| |_|\___|
                               | |
                               |*/
    fun markAsSeen(event: EventFull) {
        event.seen = true
        startCoroutineTimer(500L, 0L) {
            app.db.metadataDao().setSeen(event.profileId, event, true)
        }
    }

    fun setEventTopic(
        title: TextView,
        event: EventFull,
        showType: Boolean = true,
        doneIconColor: Int? = null
    ) {
        val topicSpan = event.topicHtml

        title.text = listOfNotNull(
            if (event.addedManually) "{cmd-clipboard-edit-outline} " else null,
            if (event.hasNotes()) "{cmd-playlist-edit} " else null,
            if (showType) "${event.typeName ?: "wydarzenie"} - " else null,
            topicSpan,
        ).concat()

        title.setCompoundDrawables(
            null,
            null,
            if (event.isDone) IconicsDrawable(title.context).apply {
                icon = CommunityMaterial.Icon.cmd_check
                colorInt = doneIconColor ?: R.color.md_green_500.resolveColor(title.context)
                sizeDp = 24
            } else null,
            null
        )
    }

    fun setLegendText(legend: IconicsTextView, event: EventFull) {
        legend.text = listOfNotNull(
            if (event.addedManually) R.string.legend_event_added_manually else null,
            if (event.isDone) R.string.legend_event_is_done else null,
            if (event.hasNotes()) R.string.legend_notes_added else null,
        ).map { legend.context.getString(it) }.join("\n")
        legend.isVisible = legend.text.isNotBlank()
    }
}
