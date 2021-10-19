/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.ui.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.applandeo.materialcalendarview.EventDay
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.EventType
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaCalendarBinding
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaDefaultBinding
import pl.szczodrzynski.edziennik.ui.dialogs.settings.AgendaConfigDialog
import pl.szczodrzynski.edziennik.ui.event.EventManualDialog
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import java.util.*
import kotlin.coroutines.CoroutineContext

class AgendaFragment : Fragment(), CoroutineScope {

    private lateinit var activity: MainActivity
    private lateinit var b: ViewDataBinding

    private val app by lazy { activity.app }

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private var type: Int = Profile.AGENDA_DEFAULT

    private var agendaDefault: AgendaFragmentDefault? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (getActivity() == null || context == null) return null
        activity = getActivity() as MainActivity
        context?.theme?.applyStyle(Themes.appTheme, true)
        type = app.config.forProfile().ui.agendaViewType
        b = when (type) {
            Profile.AGENDA_DEFAULT -> FragmentAgendaDefaultBinding.inflate(inflater, container, false)
            Profile.AGENDA_CALENDAR -> FragmentAgendaCalendarBinding.inflate(inflater, container, false)
            else -> return null
        }
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        activity.bottomSheet.prependItems(
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_add_event)
                        .withDescription(R.string.menu_add_event_desc)
                        .withIcon(SzkolnyFont.Icon.szf_calendar_plus_outline)
                        .withOnClickListener {
                            activity.bottomSheet.close()
                            EventManualDialog(
                                activity,
                                app.profileId,
                                defaultDate = AgendaFragmentDefault.selectedDate
                            ).show()
                        },
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_agenda_config)
                        .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
                        .withOnClickListener {
                            activity.bottomSheet.close()
                            AgendaConfigDialog(activity, true, null, null).show()
                        },
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_agenda_change_view)
                        .withIcon(if (type == Profile.AGENDA_DEFAULT) CommunityMaterial.Icon.cmd_calendar_outline else CommunityMaterial.Icon2.cmd_format_list_bulleted_square)
                        .withOnClickListener {
                            activity.bottomSheet.close()
                            type =
                                if (type == Profile.AGENDA_DEFAULT) Profile.AGENDA_CALENDAR else Profile.AGENDA_DEFAULT
                            app.config.forProfile().ui.agendaViewType = type
                            activity.reloadTarget()
                        },
                BottomSheetSeparatorItem(true),
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                        .withOnClickListener {
                            launch {
                                activity.bottomSheet.close()
                                withContext(Dispatchers.Default) {
                                    App.db.metadataDao()
                                        .setAllSeen(app.profileId, Metadata.TYPE_EVENT, true)
                                }
                                Toast.makeText(
                                    activity,
                                    R.string.main_menu_mark_as_read_success,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
        )

        activity.navView.bottomBar.fabEnable = true
        activity.navView.bottomBar.fabExtendedText = getString(R.string.add)
        activity.navView.bottomBar.fabIcon = CommunityMaterial.Icon3.cmd_plus
        activity.navView.setFabOnClickListener {
            EventManualDialog(
                activity,
                app.profileId,
                defaultDate = AgendaFragmentDefault.selectedDate
            ).show()
        }

        activity.gainAttention()
        activity.gainAttentionFAB()

        when (type) {
            Profile.AGENDA_DEFAULT -> createDefaultAgendaView()
            Profile.AGENDA_CALENDAR -> createCalendarAgendaView()
        }
    }

    private suspend fun checkEventTypes() {
        withContext(Dispatchers.Default) {
            val eventTypes = app.db.eventTypeDao().getAllNow(app.profileId).map {
                it.id
            }
            val defaultEventTypes = EventType.getTypeColorMap().keys
            if (!eventTypes.containsAll(defaultEventTypes)) {
                app.db.eventTypeDao().addDefaultTypes(activity, app.profileId)
            }
        }
    }

    private fun createDefaultAgendaView() { (b as? FragmentAgendaDefaultBinding)?.let { b -> launch {
        if (!isAdded)
            return@launch
        checkEventTypes()
        delay(500)

        agendaDefault = AgendaFragmentDefault(activity, app, b)
        agendaDefault?.initView(this@AgendaFragment)
    }}}

    private fun createCalendarAgendaView() { (b as? FragmentAgendaCalendarBinding)?.let { b -> launch {
        checkEventTypes()
        delay(300)

        val dayList = mutableListOf<EventDay>()

        val events = withContext(Dispatchers.Default) { app.db.eventDao().getAllNow(app.profileId) }
        val unreadEventDates = mutableSetOf<Int>()

        events.forEach { event ->
            val eventIcon = IconicsDrawable(activity).apply {
                icon = CommunityMaterial.Icon.cmd_checkbox_blank_circle
                sizeDp = 10
                colorInt = event.eventColor
            }

            dayList.add(EventDay(event.startTimeCalendar, eventIcon))

            if (!event.seen) unreadEventDates.add(event.date.value)
        }

        b.agendaCalendarView.setEvents(dayList)
        b.agendaCalendarView.setOnDayClickListener { day -> this@AgendaFragment.launch {
            val date = Date.fromCalendar(day.calendar)

            if (date.value in unreadEventDates) {
                withContext(Dispatchers.Default) { app.db.eventDao().setSeenByDate(app.profileId, date, true) }
                unreadEventDates.remove(date.value)
            }

            DayDialog(activity, app.profileId, date).show()
        }}

        b.progressBar.visibility = View.GONE
    }}}
}
