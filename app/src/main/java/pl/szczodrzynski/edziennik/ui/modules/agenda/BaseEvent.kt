/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-9.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda

import com.github.tibolte.agendacalendarview.models.CalendarEvent
import com.github.tibolte.agendacalendarview.models.IDayItem
import com.github.tibolte.agendacalendarview.models.IWeekItem
import java.util.*

open class BaseEvent(
    private val id: Long,
    private val time: Calendar,
    private val color: Int,
    private val showBadge: Boolean
) : CalendarEvent {

    override fun copy() = BaseEvent(id, time, color, showBadge)

    private lateinit var date: Calendar
    override fun getInstanceDay() = date
    override fun setInstanceDay(value: Calendar) {
        date = value
    }

    private lateinit var dayReference: IDayItem
    override fun getDayReference() = dayReference
    override fun setDayReference(value: IDayItem) {
        dayReference = value
    }

    private lateinit var weekReference: IWeekItem
    override fun getWeekReference() = weekReference
    override fun setWeekReference(value: IWeekItem) {
        weekReference = value
    }

    override fun getId() = id
    override fun getStartTime() = time
    override fun getEndTime() = time
    override fun getTitle() = ""
    override fun getDescription() = ""
    override fun getLocation() = ""
    override fun getColor() = color
    override fun getTextColor() = 0
    override fun getShowBadge() = showBadge
    override fun isPlaceholder() = false
    override fun isAllDay() = false

    override fun setId(value: Long) = Unit
    override fun setStartTime(value: Calendar) = Unit
    override fun setEndTime(value: Calendar) = Unit
    override fun setTitle(value: String) = Unit
    override fun setDescription(value: String) = Unit
    override fun setLocation(value: String) = Unit
    override fun setTextColor(value: Int) = Unit
    override fun setShowBadge(value: Boolean) = Unit
    override fun setPlaceholder(value: Boolean) = Unit
    override fun setAllDay(value: Boolean) = Unit
}
