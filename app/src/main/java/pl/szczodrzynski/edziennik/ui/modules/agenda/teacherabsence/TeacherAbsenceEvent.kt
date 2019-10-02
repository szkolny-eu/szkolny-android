package pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence

import com.github.tibolte.agendacalendarview.models.CalendarEvent
import com.github.tibolte.agendacalendarview.models.IDayItem
import com.github.tibolte.agendacalendarview.models.IWeekItem
import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.*

class TeacherAbsenceEvent : CalendarEvent {
    /**
     * Id of the event.
     */
    private var mId: Long = 0
    /**
     * Color to be displayed in the agenda view.
     */
    private var mColor: Int = 0
    /**
     * Text color displayed on the background color
     */
    private var mTextColor: Int = 0
    /**
     * Calendar instance helping sorting the events per section in the agenda view.
     */
    private var mInstanceDay: Calendar? = null
    /**
     * Start time of the event.
     */
    private var mStartTime: Calendar? = null
    /**
     * End time of the event.
     */
    private var mEndTime: Calendar? = null
    /**
     * References to a DayItem instance for that event, used to link interaction between the
     * calendar view and the agenda view.
     */
    private var mDayReference: IDayItem? = null
    /**
     * References to a WeekItem instance for that event, used to link interaction between the
     * calendar view and the agenda view.
     */
    private var mWeekReference: IWeekItem? = null


    private var profileId: Int = 0
    var teacherAbsenceDate: Date? = null
    var teacherAbsenceCount: Int = 0

    constructor(calendarEvent: TeacherAbsenceEvent) {
        this.mId = calendarEvent.id
        this.mColor = calendarEvent.color
        this.mTextColor = calendarEvent.textColor
        this.mStartTime = calendarEvent.startTime
        this.mEndTime = calendarEvent.endTime
        this.profileId = calendarEvent.profileId
        this.teacherAbsenceDate = calendarEvent.teacherAbsenceDate
        this.teacherAbsenceCount = calendarEvent.teacherAbsenceCount
    }

    constructor(mId: Long, mColor: Int, mTextColor: Int, mStartTime: Calendar, mEndTime: Calendar, profileId: Int, teacherAbsenceDate: Date, teacherAbsenceCount: Int) {
        this.mId = mId
        this.mColor = mColor
        this.mTextColor = mTextColor
        this.mStartTime = mStartTime
        this.mEndTime = mEndTime
        this.profileId = profileId
        this.teacherAbsenceDate = teacherAbsenceDate
        this.teacherAbsenceCount = teacherAbsenceCount
    }

    override fun setPlaceholder(placeholder: Boolean) {

    }

    override fun isPlaceholder(): Boolean {
        return false
    }

    override fun getLocation(): String? {
        return null
    }

    override fun setLocation(mLocation: String) {

    }

    override fun getId(): Long {
        return mId
    }

    override fun setId(mId: Long) {
        this.mId = mId
    }

    override fun getShowBadge(): Boolean {
        return false
    }

    override fun setShowBadge(mShowBadge: Boolean) {

    }

    override fun getTextColor(): Int {
        return mTextColor
    }

    override fun setTextColor(mTextColor: Int) {
        this.mTextColor = mTextColor
    }

    override fun getDescription(): String? {
        return null
    }

    override fun setDescription(mDescription: String) {

    }

    override fun isAllDay(): Boolean {
        return false
    }

    override fun setAllDay(allDay: Boolean) {

    }

    override fun getStartTime(): Calendar? {
        return mStartTime
    }

    override fun setStartTime(mStartTime: Calendar) {
        this.mStartTime = mStartTime
    }

    override fun getEndTime(): Calendar? {
        return mEndTime
    }

    override fun setEndTime(mEndTime: Calendar) {
        this.mEndTime = mEndTime
    }

    override fun getTitle(): String? {
        return null
    }

    override fun setTitle(mTitle: String) {

    }

    override fun getInstanceDay(): Calendar? {
        return mInstanceDay
    }

    override fun setInstanceDay(mInstanceDay: Calendar) {
        this.mInstanceDay = mInstanceDay
        this.mInstanceDay!!.set(Calendar.HOUR, 0)
        this.mInstanceDay!!.set(Calendar.MINUTE, 0)
        this.mInstanceDay!!.set(Calendar.SECOND, 0)
        this.mInstanceDay!!.set(Calendar.MILLISECOND, 0)
        this.mInstanceDay!!.set(Calendar.AM_PM, 0)
    }

    override fun getDayReference(): IDayItem? {
        return mDayReference
    }

    override fun setDayReference(mDayReference: IDayItem) {
        this.mDayReference = mDayReference
    }

    override fun getWeekReference(): IWeekItem? {
        return mWeekReference
    }

    override fun setWeekReference(mWeekReference: IWeekItem) {
        this.mWeekReference = mWeekReference
    }

    override fun copy(): CalendarEvent {
        return TeacherAbsenceEvent(this)
    }

    override fun getColor(): Int {
        return mColor
    }
}
