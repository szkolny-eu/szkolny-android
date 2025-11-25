/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-28.
 */

package pl.szczodrzynski.edziennik.ui.home.cards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.plusAssign
import androidx.core.view.setMargins
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.CardHomeEventsBinding
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.dialogs.settings.HomeConfigDialog
import pl.szczodrzynski.edziennik.ui.event.EventDetailsDialog
import pl.szczodrzynski.edziennik.ui.event.EventListAdapter
import pl.szczodrzynski.edziennik.ui.event.EventManualDialog
import pl.szczodrzynski.edziennik.ui.home.HomeCard
import pl.szczodrzynski.edziennik.ui.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.home.HomeFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.colorAttr
import kotlin.coroutines.CoroutineContext

class HomeEventsCard(
        override val id: Int,
        val app: App,
        val activity: MainActivity,
        val fragment: HomeFragment,
        val profile: Profile
) : HomeCard, CoroutineScope {
    companion object {
        private const val TAG = "HomeEventsCard"
    }

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var adapter: EventListAdapter

    override fun bind(position: Int, holder: HomeCardAdapter.ViewHolder) { launch {
        holder.root.removeAllViews()
        val b = CardHomeEventsBinding.inflate(LayoutInflater.from(holder.root.context))
        b.root.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(8.dp)
        }
        holder.root += b.root

        b.settings.setImageDrawable(
            IconicsDrawable(activity, CommunityMaterial.Icon.cmd_cog_outline).apply {
                colorAttr(activity, R.attr.colorIcon)
                sizeDp = 24
            }
        )
        b.settings.onClick {
            HomeConfigDialog(activity, reloadOnDismiss = true).show()
        }

        adapter = EventListAdapter(
                activity,
                simpleMode = true,
                showWeekDay = true,
                showDate = true,
                showType = !profile.config.ui.agendaSubjectImportant,
                showTypeColor = true,
                showTime = false,
                showSubject = profile.config.ui.agendaSubjectImportant,
                markAsSeen = false,
                onEventClick = {
                    EventDetailsDialog(
                            activity,
                            it
                    ).show()
                },
                onEventEditClick = {
                    EventManualDialog(
                            activity,
                            it.profileId,
                            editingEvent = it
                    ).show()
                }
        )

        val limit = app.profile.config.ui.homeEventsLimit
        val days = app.profile.config.ui.homeEventsWeeks * 7
        val toDate = Date.getToday().stepForward(0, 0, days)

        app.db.eventDao().getNearestNotDone(profile.id, Date.getToday(), limit).observe(activity, Observer { eventsAll ->
            val events = eventsAll.filter { it.date <= toDate }

            events.forEach {
                it.filterNotes()
            }

            adapter.setAllItems(events)
            if (b.eventsView.adapter == null) {
                b.eventsView.adapter = adapter
                b.eventsView.apply {
                    isNestedScrollingEnabled = false
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(SimpleDividerItemDecoration(context))
                }
            }
            adapter.notifyDataSetChanged()

            if (events != null && events.isNotEmpty()) {
                b.eventsView.visibility = View.VISIBLE
                b.eventsNoData.visibility = View.GONE
            } else {
                b.eventsView.visibility = View.GONE
                b.eventsNoData.visibility = View.VISIBLE
            }
        })

        holder.root.onClick {
            activity.navigate(navTarget = NavTarget.AGENDA)
        }
    }}

    override fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder) = Unit
}
