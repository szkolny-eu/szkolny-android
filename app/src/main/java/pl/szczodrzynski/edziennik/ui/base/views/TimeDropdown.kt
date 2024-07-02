/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-23.
 */

package pl.szczodrzynski.edziennik.ui.base.views

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.LessonRange
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.ext.asItalicSpannable
import pl.szczodrzynski.edziennik.ext.asStrikethroughSpannable
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ext.listOfNotEmpty
import pl.szczodrzynski.edziennik.utils.TextInputDropDown
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class TimeDropdown : TextInputDropDown {
    companion object {
        const val DISPLAY_LESSON_RANGES = 0
        const val DISPLAY_LESSONS = 1
    }

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
    var showAllDay = false
    var showCustomTime = true
    var displayMode = DISPLAY_LESSON_RANGES
    var lessonsDate: Date? = null
    var onTimeSelected: ((startTime: Time?, endTime: Time?, lessonNumber: Int?) -> Unit)? = null
    var onLessonSelected: ((lesson: LessonFull) -> Unit)? = null

    override fun create(context: Context) {
        super.create(context)
        isEnabled = false
    }

    suspend fun loadItems(): Boolean {
        var noTimetable = false
        val hours = withContext(Dispatchers.Default) {
            val hours = mutableListOf<Item>()

            if (showAllDay) {
                hours += Item(
                        0L,
                        context.getString(R.string.dialog_event_manual_all_day),
                        tag = 0L
                )
            }

            if (showCustomTime) {
                hours += Item(
                        -1,
                        context.getString(R.string.dialog_event_manual_custom_time),
                        tag = -1L
                )
            }

            if (displayMode == DISPLAY_LESSON_RANGES) {
                val lessonRanges = db.lessonRangeDao().getAllNow(profileId)

                hours += lessonRanges.map { Item(
                        it.startTime.value.toLong(),
                        context.getString(R.string.timetable_manual_dialog_time_format, it.startTime.stringHM, it.lessonNumber),
                        tag = it
                ) }
            }
            else if (displayMode == DISPLAY_LESSONS && lessonsDate != null) {
                val lessons = db.timetableDao().getAllForDateNow(profileId, lessonsDate!!)

                if (lessons.isEmpty()) {
                    hours += Item(
                            -2L,
                            context.getString(R.string.dialog_event_manual_no_timetable),
                            tag = -2L
                    )
                    noTimetable = true
                    return@withContext hours
                }

                hours += lessons.map { lesson ->
                    if (lesson.type == Lesson.TYPE_NO_LESSONS) {
                        // indicate there are no lessons this day
                        return@map Item(
                                -2L,
                                context.getString(R.string.dialog_event_manual_no_lessons),
                                tag = -2L
                        )
                    }
                    // create the lesson caption
                    val text = listOfNotEmpty(
                            lesson.displayStartTime?.stringHM ?: "",
                            if (lesson.displaySubjectName != null) "-" else "",
                            lesson.displaySubjectName?.let {
                                when {
                                    lesson.type == Lesson.TYPE_CANCELLED
                                            || lesson.type == Lesson.TYPE_SHIFTED_SOURCE -> it.asStrikethroughSpannable()
                                    lesson.type != Lesson.TYPE_NORMAL -> it.asItalicSpannable()
                                    else -> it
                                }
                            } ?: ""
                    )
                    // add an item with LessonFull as the tag
                    return@map Item(
                            lesson.displayStartTime?.value?.toLong() ?: -1,
                            text.concat(" "),
                            tag = lesson
                    )
                }
            }
            hours
        }

        clear().append(hours)
        isEnabled = true

        setOnChangeListener {
            when (it.tag) {
                -2L -> {
                    // no lessons this day
                    deselect()
                    false
                }
                -1L -> {
                    // custom start hour
                    pickerDialog()
                    false
                }
                0L -> {
                    // selected all day
                    onTimeSelected?.invoke(null, null, null)
                    true
                }
                is LessonFull -> {
                    // selected a specific lesson
                    onLessonSelected?.invoke(it.tag)
                    true
                }
                is LessonRange -> {
                    // selected a lesson range
                    onTimeSelected?.invoke(it.tag.startTime, it.tag.endTime, it.tag.lessonNumber)
                    true
                }
                is Time -> {
                    // selected a time
                    onTimeSelected?.invoke(it.tag, null, null)
                    true
                }
                else -> false
            }
        }

        return !noTimetable
    }

    private fun pickerDialog() {
        val time = (getSelected() as? Pair<*, *>)?.first as? Time ?: Time.getNow()

        MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(time.hour)
            .setMinute(time.minute)
            .build()
            .also { dialog ->
                dialog.addOnPositiveButtonClickListener {
                    val timeSelected = Time(dialog.hour, dialog.minute, 0)
                    selectTime(timeSelected)
                    onTimeSelected?.invoke(timeSelected, null, null)
                }
            }
            .show(activity!!.supportFragmentManager, "TimeDropdown")
    }

    fun selectTime(time: Time) {
        if (select(time.value.toLong()) == null)
            select(Item(
                    time.value.toLong(),
                    time.stringHM,
                    tag = time
            ))
    }

    fun selectDefault(time: Time?) {
        if (time == null || selected != null)
            return
        selectTime(time)
    }

    /**
     * Get the currently selected time.
     * ### Returns:
     * - null if no valid time is selected
     * - 0L if 'all day' is selected
     * - a [Pair] of [Time] and [Time]? - the selected time object, if [displayMode] == [DISPLAY_LESSONS] or [showCustomTime]
     * - [LessonRange] - the selected lesson range object, if [displayMode] == [DISPLAY_LESSON_RANGES]
     */
    fun getSelected(): Any? {
        return when (val tag = selected?.tag) {
            0L -> 0L
            is LessonFull ->
                if (tag.displayStartTime != null)
                    tag.displayStartTime!! to tag.displayEndTime
                else
                    null
            is LessonRange -> tag
            is Time -> tag to null
            else -> null
        }
    }
}
