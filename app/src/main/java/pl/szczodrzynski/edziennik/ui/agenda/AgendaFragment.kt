/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.ui.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.applandeo.materialcalendarview.EventDay
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaCalendarBinding
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaDefaultBinding
import pl.szczodrzynski.edziennik.ext.toDrawable
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.dialogs.settings.AgendaConfigDialog
import pl.szczodrzynski.edziennik.ui.event.EventManualDialog
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem

class AgendaFragment : BaseFragment<ViewBinding, MainActivity>(
    inflater = null,
) {

    override fun inflate(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean,
    ) = when (app.profile.config.ui.agendaViewType) {
        Profile.AGENDA_DEFAULT -> FragmentAgendaDefaultBinding.inflate(inflater, parent, false)
        Profile.AGENDA_CALENDAR -> FragmentAgendaCalendarBinding.inflate(inflater, parent, false)
        else -> null
    }

    override fun getFab() = R.string.add to CommunityMaterial.Icon3.cmd_plus
    override fun getMarkAsReadType() = MetadataType.EVENT
    override fun getBottomSheetItems() = listOf(
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
                AgendaConfigDialog(activity, true).show()
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_agenda_change_view)
            .withIcon(
                if (app.profile.config.ui.agendaViewType == Profile.AGENDA_DEFAULT)
                    CommunityMaterial.Icon.cmd_calendar_outline
                else
                    CommunityMaterial.Icon2.cmd_format_list_bulleted_square
            )
            .withOnClickListener {
                activity.bottomSheet.close()
                app.profile.config.ui.agendaViewType =
                    if (app.profile.config.ui.agendaViewType == Profile.AGENDA_DEFAULT)
                        Profile.AGENDA_CALENDAR
                    else
                        Profile.AGENDA_DEFAULT
                activity.reloadTarget()
            },
    )

    private var agendaDefault: AgendaFragmentDefault? = null

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        when (app.profile.config.ui.agendaViewType) {
            Profile.AGENDA_DEFAULT -> createDefaultAgendaView(
                b as? FragmentAgendaDefaultBinding ?: return
            )
            Profile.AGENDA_CALENDAR -> createCalendarAgendaView(
                b as? FragmentAgendaCalendarBinding ?: return
            )
        }
    }

    override suspend fun onFabClick() {
        EventManualDialog(
            activity,
            app.profileId,
            defaultDate = AgendaFragmentDefault.selectedDate
        ).show()
    }

    private suspend fun checkEventTypes() {
        withContext(Dispatchers.Default) {
            app.db.eventTypeDao().getAllWithDefaults(app.profile)
        }
    }

    private suspend fun createDefaultAgendaView(b: FragmentAgendaDefaultBinding) {
        if (!isAdded)
            return
        checkEventTypes()
        delay(500)

        agendaDefault = AgendaFragmentDefault(activity, app, b)
        agendaDefault?.initView(this@AgendaFragment)
    }

    private suspend fun createCalendarAgendaView(b: FragmentAgendaCalendarBinding) {
        checkEventTypes()
        delay(300)

        val dayList = mutableListOf<EventDay>()

        val events = withContext(Dispatchers.IO) {
            app.db.eventDao().getAllNow(app.profileId)
        }
        val unreadEventDates = mutableSetOf<Int>()

        events.forEach { event ->
            val eventIcon = CommunityMaterial.Icon.cmd_checkbox_blank_circle
                .toDrawable(event.eventColor, sizeDp = 10)

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
    }
}
