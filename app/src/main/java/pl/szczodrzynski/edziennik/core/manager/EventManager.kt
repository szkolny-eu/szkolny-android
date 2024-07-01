/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-3.
 */

package pl.szczodrzynski.edziennik.core.manager

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
        showSubject: Boolean = false,
        showNotes: Boolean = true,
        doneIconColor: Int? = null
    ) {
        val topicSpan = event.getNoteSubstituteText(showNotes) ?: event.topicHtml
        val hasReplacingNotes = event.hasReplacingNotes()

        title.text = listOfNotNull(
            if (event.addedManually && !event.isSharedReceived) "{cmd-calendar-edit} " else null,
            if (event.isSharedReceived) "{cmd-share-variant} " else null,
            if (event.hasNotes() && hasReplacingNotes && showNotes) "{cmd-swap-horizontal} " else null,
            if (event.hasNotes() && !hasReplacingNotes && showNotes) "{cmd-playlist-edit} " else null,
            if (showType) "${event.typeName ?: "wydarzenie"} - " else null,
            if (showSubject) event.subjectLongName?.plus(" - ") else null,
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

    fun setLegendText(legend: IconicsTextView, event: EventFull, showNotes: Boolean = true) {
        legend.text = listOfNotNull(
            if (event.addedManually) R.string.legend_event_added_manually else null,
            if (event.isSharedSent) R.string.legend_event_shared_sent else null,
            if (event.isSharedReceived) R.string.legend_event_shared_received else null,
            if (event.isDone) R.string.legend_event_is_done else null,
            if (showNotes) NoteManager.getLegendText(event) else null,
        ).map { legend.context.getString(it) }.join("\n")
        legend.isVisible = legend.text.isNotBlank()
    }
}
