/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.views

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.observeOnce
import pl.szczodrzynski.edziennik.resolveAttr
import pl.szczodrzynski.edziennik.utils.TextInputDropDown
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week

class DateDropdown : TextInputDropDown {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val activity: AppCompatActivity?
        get() {
            var context: Context? = context ?: return null
            if (context is AppCompatActivity) return context
            while (context is ContextWrapper) {
                if (context is AppCompatActivity)
                    return context
                context = context.baseContext
            }
            return null
        }

    lateinit var db: AppDb
    var profileId: Int = 0
    var showWeekDays = false
    var showDays = true
    var showOtherDate = true
    var nextLessonSubjectId: Long? = null
    var nextLessonSubjectName: String? = null
    var nextLessonTeamId: Long? = null
    var onDateSelected: ((date: Date, lesson: LessonFull?) -> Unit)? = null
    var onWeekDaySelected: ((weekDay: Int) -> Unit)? = null

    override fun create(context: Context) {
        super.create(context)
        isEnabled = false
    }

    suspend fun loadItems() {
        val date = Date.getToday()
        val today = date.value
        var weekDay = date.weekDay

        val dates = withContext(Dispatchers.Default) {
            val dates = mutableListOf<Item>()

            nextLessonSubjectId?.let {
                // item choosing the next lesson of specific subject - relative to selected date
                dates += Item(
                        -it,
                        context.getString(R.string.dialog_event_manual_date_next_lesson, nextLessonSubjectName),
                        tag = nextLessonSubjectName
                )
            }

            if (showWeekDays) {
                for (i in Week.MONDAY..Week.SUNDAY) {
                    dates += Item(
                            i.toLong(),
                            Week.getFullDayName(i),
                            tag = i
                    )
                }
            }

            if (showDays) {
                // TODAY
                dates += Item(
                        date.value.toLong(),
                        context.getString(R.string.dialog_event_manual_date_today, date.formattedString),
                        tag = date.clone()
                )

                // TOMORROW
                if (weekDay < 4) {
                    date.stepForward(0, 0, 1)
                    weekDay++
                    dates += Item(
                            date.value.toLong(),
                            context.getString(R.string.dialog_event_manual_date_tomorrow, date.formattedString),
                            tag = date.clone()
                    )
                }
                // REMAINING SCHOOL DAYS OF THE CURRENT WEEK
                while (weekDay < 4) {
                    date.stepForward(0, 0, 1) // step one day forward
                    weekDay++
                    dates += Item(
                            date.value.toLong(),
                            context.getString(R.string.dialog_event_manual_date_this_week, Week.getFullDayName(weekDay), date.formattedString),
                            tag = date.clone()
                    )
                }
                // go to next week Monday
                date.stepForward(0, 0, -weekDay + 7)
                weekDay = 0
                // ALL SCHOOL DAYS OF THE NEXT WEEK
                while (weekDay < 4) {
                    dates += Item(
                            date.value.toLong(),
                            context.getString(R.string.dialog_event_manual_date_next_week, Week.getFullDayName(weekDay), date.formattedString),
                            tag = date.clone()
                    )
                    date.stepForward(0, 0, 1) // step one day forward
                    weekDay++
                }
            }

            if (showOtherDate) {
                dates += Item(
                        -1L,
                        context.getString(R.string.dialog_event_manual_date_other),
                        tag = -1L
                )
            }
            dates
        }

        clear().append(dates)
        isEnabled = true

        setOnChangeListener { item ->
            when (item.tag) {
                -1L -> {
                    pickerDialog()
                    false
                }
                is Date -> {
                    onDateSelected?.invoke(item.tag, null)
                    true
                }
                is Int -> {
                    onWeekDaySelected?.invoke(item.tag)
                    true
                }
                is String -> {
                    /* next lesson of subject */
                    activity ?: return@setOnChangeListener false
                    val subjectId = -item.id
                    val startDate = getSelected() as? Date ?: Date.getToday()
                    when (nextLessonTeamId) {
                        null -> db.timetableDao().getNextWithSubject(profileId, startDate, subjectId)
                        else -> db.timetableDao().getNextWithSubjectAndTeam(profileId, startDate, subjectId, nextLessonTeamId ?: -1)
                    }.observeOnce(activity!!, Observer {
                        if (it == null) {
                            Toast.makeText(context, R.string.dropdown_date_no_more_lessons, Toast.LENGTH_LONG).show()
                            return@Observer
                        }
                        val lessonDate = it.displayDate ?: return@Observer
                        selectDate(lessonDate)
                        onDateSelected?.invoke(lessonDate, it)
                    })
                    false
                }
                else -> false
            }
        }
    }

    fun pickerDialog() {
        val date = getSelected() as? Date ?: Date.getToday()

        DatePickerDialog
                .newInstance({ _, year, monthOfYear, dayOfMonth ->
                    val dateSelected = Date(year, monthOfYear, dayOfMonth)
                    selectDate(dateSelected)
                    onDateSelected?.invoke(dateSelected, null)
                }, date.year, date.month, date.day)
                .apply {
                    this@DateDropdown.activity ?: return@apply
                    accentColor = R.attr.colorPrimary.resolveAttr(this@DateDropdown.activity)
                    show(this@DateDropdown.activity!!.supportFragmentManager, "DatePickerDialog")
                }
    }

    fun selectDate(date: Date) {
        if (select(date) == null)
            select(Item(
                    date.value.toLong(),
                    date.formattedString,
                    tag = date
            ))
    }
    fun selectWeekDay(weekDay: Int) {
        if (select(tag = weekDay) == null)
            select(Item(
                    weekDay.toLong(),
                    Week.getFullDayName(weekDay),
                    tag = weekDay
            ))
    }

    fun selectDefault(date: Date?) {
        if (date == null || selected != null)
            return
        selectDate(date)
    }
    fun selectDefault(weekDay: Int?) {
        if (weekDay == null || selected != null)
            return
        selectWeekDay(weekDay)
    }

    /**
     * Get the currently selected date.
     * ### Returns:
     * - null if no valid date is selected
     * - [Date] - the selected date, if [showDays] or [showOtherDate] == true
     * - [Int] - the selected week day, if [showWeekDays] == true
     */
    fun getSelected(): Any? {
        return when (val tag = selected?.tag) {
            is Date -> tag
            is Int -> tag
            else -> null
        }
    }
}
