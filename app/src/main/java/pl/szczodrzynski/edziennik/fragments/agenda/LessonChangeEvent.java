package pl.szczodrzynski.edziennik.fragments.agenda;

import com.github.tibolte.agendacalendarview.models.BaseCalendarEvent;
import com.github.tibolte.agendacalendarview.models.CalendarEvent;
import com.github.tibolte.agendacalendarview.models.IDayItem;
import com.github.tibolte.agendacalendarview.models.IWeekItem;

import java.util.Calendar;

import pl.szczodrzynski.edziennik.models.Date;

public class LessonChangeEvent implements CalendarEvent {

    /**
     * Id of the event.
     */
    private long mId;
    /**
     * Color to be displayed in the agenda view.
     */
    private int mColor;
    /**
     * Text color displayed on the background color
     */
    private int mTextColor;
    /**
     * Calendar instance helping sorting the events per section in the agenda view.
     */
    private Calendar mInstanceDay;
    /**
     * Start time of the event.
     */
    private Calendar mStartTime;
    /**
     * End time of the event.
     */
    private Calendar mEndTime;
    /**
     * References to a DayItem instance for that event, used to link interaction between the
     * calendar view and the agenda view.
     */
    private IDayItem mDayReference;
    /**
     * References to a WeekItem instance for that event, used to link interaction between the
     * calendar view and the agenda view.
     */
    private IWeekItem mWeekReference;


    private int profileId;
    private Date lessonChangeDate;
    private int lessonChangeCount;

    public LessonChangeEvent(LessonChangeEvent calendarEvent) {
        this.mId = calendarEvent.getId();
        this.mColor = calendarEvent.getColor();
        this.mTextColor = calendarEvent.getTextColor();
        this.mStartTime = calendarEvent.getStartTime();
        this.mEndTime = calendarEvent.getEndTime();
        this.profileId = calendarEvent.getProfileId();
        this.lessonChangeDate = calendarEvent.getLessonChangeDate();
        this.lessonChangeCount = calendarEvent.getLessonChangeCount();
    }

    public LessonChangeEvent(long mId, int mColor, int mTextColor, Calendar mStartTime, Calendar mEndTime, int profileId, Date lessonChangeDate, int lessonChangeCount) {
        this.mId = mId;
        this.mColor = mColor;
        this.mTextColor = mTextColor;
        this.mStartTime = mStartTime;
        this.mEndTime = mEndTime;
        this.profileId = profileId;
        this.lessonChangeDate = lessonChangeDate;
        this.lessonChangeCount = lessonChangeCount;
    }

    public int getProfileId() {
        return profileId;
    }

    public Date getLessonChangeDate() {
        return lessonChangeDate;
    }

    public int getLessonChangeCount() {
        return lessonChangeCount;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public void setLessonChangeDate(Date lessonChangeDate) {
        this.lessonChangeDate = lessonChangeDate;
    }

    public void setLessonChangeCount(int lessonChangeCount) {
        this.lessonChangeCount = lessonChangeCount;
    }

    @Override
    public void setPlaceholder(boolean placeholder) {

    }

    @Override
    public boolean isPlaceholder() {
        return false;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public void setLocation(String mLocation) {

    }

    @Override
    public long getId() {
        return mId;
    }

    @Override
    public void setId(long mId) {
        this.mId = mId;
    }

    @Override
    public boolean getShowBadge() {
        return false;
    }

    @Override
    public void setShowBadge(boolean mShowBadge) {

    }

    @Override
    public int getTextColor() {
        return mTextColor;
    }

    @Override
    public void setTextColor(int mTextColor) {
        this.mTextColor = mTextColor;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void setDescription(String mDescription) {

    }

    @Override
    public boolean isAllDay() {
        return false;
    }

    @Override
    public void setAllDay(boolean allDay) {

    }

    @Override
    public Calendar getStartTime() {
        return mStartTime;
    }

    @Override
    public void setStartTime(Calendar mStartTime) {
        this.mStartTime = mStartTime;
    }

    @Override
    public Calendar getEndTime() {
        return mEndTime;
    }

    @Override
    public void setEndTime(Calendar mEndTime) {
        this.mEndTime = mEndTime;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public void setTitle(String mTitle) {

    }

    @Override
    public Calendar getInstanceDay() {
        return mInstanceDay;
    }

    @Override
    public void setInstanceDay(Calendar mInstanceDay) {
        this.mInstanceDay = mInstanceDay;
        this.mInstanceDay.set(Calendar.HOUR, 0);
        this.mInstanceDay.set(Calendar.MINUTE, 0);
        this.mInstanceDay.set(Calendar.SECOND, 0);
        this.mInstanceDay.set(Calendar.MILLISECOND, 0);
        this.mInstanceDay.set(Calendar.AM_PM, 0);
    }

    @Override
    public IDayItem getDayReference() {
        return mDayReference;
    }

    @Override
    public void setDayReference(IDayItem mDayReference) {
        this.mDayReference = mDayReference;
    }

    @Override
    public IWeekItem getWeekReference() {
        return mWeekReference;
    }

    @Override
    public void setWeekReference(IWeekItem mWeekReference) {
        this.mWeekReference = mWeekReference;
    }

    @Override
    public CalendarEvent copy() {
        return new LessonChangeEvent(this);
    }

    @Override
    public int getColor() {
        return mColor;
    }
}
