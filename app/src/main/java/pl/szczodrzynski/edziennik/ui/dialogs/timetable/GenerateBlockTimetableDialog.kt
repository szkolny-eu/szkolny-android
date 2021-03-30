/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-5
 */

package pl.szczodrzynski.edziennik.ui.dialogs.timetable

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskAllFinishedEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskErrorEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskFinishedEvent
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.databinding.DialogGenerateBlockTimetableBinding
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class GenerateBlockTimetableDialog(
        val activity: AppCompatActivity,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        const val TAG = "GenerateBlockTimetableDialog"

        private const val WIDTH_CONSTANT = 70
        private const val WIDTH_WEEKDAY = 285
        private const val WIDTH_SPACING = 15
        private const val HEIGHT_CONSTANT = 60
        private const val HEIGHT_MINUTE = 3
        private const val HEIGHT_FOOTER = 40
    }

    private val heightProfileName by lazy { if (showProfileName) 100 else 0 }

    private val app by lazy { activity.application as App }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var dialog: AlertDialog
    private lateinit var b: DialogGenerateBlockTimetableBinding

    private var showProfileName: Boolean = false
    private var showTeachersNames: Boolean = true
    private var noColors: Boolean = false

    private var enqueuedWeekDialog: AlertDialog? = null
    private var enqueuedWeekStart = Date.getToday()
    private var enqueuedWeekEnd = Date.getToday()

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        EventBus.getDefault().register(this)

        val weekCurrentStart = Week.getWeekStart()
        val weekCurrentEnd = Week.getWeekEnd()
        val weekNextStart = weekCurrentEnd.clone().stepForward(0, 0, 1)
        val weekNextEnd = weekNextStart.clone().stepForward(0, 0, 6)

        b = DialogGenerateBlockTimetableBinding.inflate(activity.layoutInflater)

        b.withChangesCurrentWeekRadio.setText(R.string.timetable_generate_current_week_format, weekCurrentStart.formattedStringShort, weekCurrentEnd.formattedStringShort)
        b.withChangesNextWeekRadio.setText(R.string.timetable_generate_next_week_format, weekNextStart.formattedStringShort, weekCurrentEnd.formattedStringShort)

        b.showProfileNameCheckbox.setOnCheckedChangeListener { _, isChecked -> showProfileName = isChecked }
        b.showTeachersNamesCheckbox.setOnCheckedChangeListener { _, isChecked -> showTeachersNames = isChecked }
        b.noColorsCheckbox.setOnCheckedChangeListener { _, isChecked -> noColors = isChecked }

        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.timetable_generate_range)
                .setView(b.root)
                .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(R.string.save, null)
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                    EventBus.getDefault().unregister(this@GenerateBlockTimetableDialog)
                }
                .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.onClick {
            when (b.weekSelectionRadioGroup.checkedRadioButtonId) {
                R.id.withChangesCurrentWeekRadio -> generateBlockTimetable(weekCurrentStart, weekCurrentEnd)
                R.id.withChangesNextWeekRadio -> generateBlockTimetable(weekNextStart, weekNextEnd)
                R.id.forSelectedWeekRadio -> selectDate()
            }
        }
    }}

    private fun selectDate() {
        MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(app.profile.getSchoolYearConstrains())
            .build()
            .apply {
                addOnPositiveButtonClickListener { millis ->
                    val dateSelected = Date.fromMillisUtc(millis)
                    generateBlockTimetable(dateSelected.weekStart, dateSelected.weekEnd)
                }
            }
            .show(activity.supportFragmentManager, TAG)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskFinishedEvent(event: ApiTaskFinishedEvent) {
        if (event.profileId == App.profileId) {
            enqueuedWeekDialog?.dismiss()
            generateBlockTimetable(enqueuedWeekStart, enqueuedWeekEnd)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskAllFinishedEvent(event: ApiTaskAllFinishedEvent) {
        enqueuedWeekDialog?.dismiss()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskErrorEvent(event: ApiTaskErrorEvent) {
        dialog.dismiss()
        enqueuedWeekDialog?.dismiss()
    }

    private fun generateBlockTimetable(weekStart: Date, weekEnd: Date) { launch {
        val weekDays = mutableListOf<MutableList<Lesson>>()
        for (i in weekStart.weekDay..weekEnd.weekDay) {
            weekDays.add(mutableListOf())
        }

        val allLessons = withContext(Dispatchers.Default) {
            app.db.timetableDao().getBetweenDatesNow(weekStart, weekEnd)
        }
        val lessonRanges = mutableMapOf<Int, Int>()

        var maxWeekDay = 5
        var minTime: Time? = null
        var maxTime: Time? = null

        val lessons: List<LessonFull> = allLessons.mapNotNull { lesson ->
            if (lesson.profileId != app.profile.id || lesson.type == Lesson.TYPE_NO_LESSONS
                    || lesson.displayDate == null || lesson.displayStartTime == null || lesson.displayEndTime == null)
                return@mapNotNull null

            if (lesson.displayDate!!.weekDay > maxWeekDay)
                maxWeekDay = lesson.displayDate!!.weekDay

            lessonRanges[lesson.displayStartTime!!.value] = lesson.displayEndTime!!.value
            weekDays[lesson.displayDate!!.weekDay].add(lesson)

            if (minTime == null || lesson.displayStartTime!! < minTime!!) {
                minTime = lesson.displayStartTime!!.clone()
            }

            if (maxTime == null || lesson.displayEndTime!! > maxTime!!) {
                maxTime = lesson.displayEndTime!!.clone()
            }

            return@mapNotNull lesson
        }

        if (lessons.isEmpty()) {
            if (enqueuedWeekDialog != null) {
                return@launch
            }
            enqueuedWeekDialog = MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.please_wait)
                    .setMessage(R.string.timetable_syncing_text)
                    .setCancelable(false)
                    .show()

            enqueuedWeekStart = weekStart
            enqueuedWeekEnd = weekEnd

            EdziennikTask.syncProfile(
                    profileId = App.profileId,
                    viewIds = listOf(
                            MainActivity.DRAWER_ITEM_TIMETABLE to 0
                    ),
                    arguments = JsonObject(
                            "weekStart" to weekStart.stringY_m_d
                    )
            ).enqueue(activity)
            return@launch
        }

        val progressDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.timetable_generate_progress_title)
                .setMessage(R.string.timetable_generate_progress_text)
                .show()

        if (minTime == null) {
            progressDialog.dismiss()
            // TODO: Toast
            return@launch
        }

        dialog.dismiss()

        val uri = withContext(Dispatchers.Default) {

            val diff = Time.diff(maxTime, minTime)

            val imageWidth = WIDTH_CONSTANT + maxWeekDay * (WIDTH_WEEKDAY + WIDTH_SPACING) - WIDTH_SPACING
            val imageHeight = heightProfileName + HEIGHT_CONSTANT + diff.inMinutes * HEIGHT_MINUTE + HEIGHT_FOOTER
            val bitmap = Bitmap.createBitmap(imageWidth + 20, imageHeight + 30, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            if (noColors) canvas.drawARGB(255, 255, 255, 255)
            else canvas.drawARGB(255, 225, 225, 225)

            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
            }

            lessons.forEach { lesson ->
                val lessonLength = Time.diff(lesson.displayEndTime, lesson.displayStartTime)
                val firstOffset = Time.diff(lesson.displayStartTime, minTime)
                val lessonWeekDay = lesson.displayDate!!.weekDay

                val left = WIDTH_CONSTANT + lessonWeekDay * (WIDTH_WEEKDAY + WIDTH_SPACING)
                val top = heightProfileName + HEIGHT_CONSTANT + firstOffset.inMinutes * HEIGHT_MINUTE

                val blockWidth = WIDTH_WEEKDAY
                val blockHeight = lessonLength.inMinutes * HEIGHT_MINUTE

                val viewWidth = 380.dp
                val viewHeight = lessonLength.inMinutes * 4.dp

                val layout = activity.layoutInflater.inflate(R.layout.row_timetable_block_item, null) as LinearLayout

                val item: LinearLayout = layout.findViewById(R.id.timetableItemLayout)
                val card: CardView = layout.findViewById(R.id.timetableItemCard)
                val subjectName: TextView = layout.findViewById(R.id.timetableItemSubjectName)
                val classroomName: TextView = layout.findViewById(R.id.timetableItemClassroomName)
                val teacherName: TextView = layout.findViewById(R.id.timetableItemTeacherName)
                val teamName: TextView = layout.findViewById(R.id.timetableItemTeamName)

                if (noColors) {
                    card.setCardBackgroundColor(Color.WHITE)
                    card.cardElevation = 0f
                    item.setBackgroundResource(R.drawable.bg_rounded_16dp_outline)
                    subjectName.setTextColor(Color.BLACK)
                    classroomName.setTextColor(0xffaaaaaa.toInt())
                    teacherName.setTextColor(0xffaaaaaa.toInt())
                    teamName.setTextColor(0xffaaaaaa.toInt())
                }

                subjectName.text = lesson.displaySubjectName ?: ""
                classroomName.text = lesson.displayClassroom ?: ""
                teacherName.text = lesson.displayTeacherName ?: ""
                teamName.text = lesson.displayTeamName ?: ""

                if (!showTeachersNames) teacherName.visibility = View.GONE

                when (lesson.type) {
                    Lesson.TYPE_NORMAL -> {
                    }
                    Lesson.TYPE_CANCELLED, Lesson.TYPE_SHIFTED_SOURCE -> {
                        card.setCardBackgroundColor(Color.BLACK)
                        subjectName.setTextColor(Color.WHITE)
                        subjectName.text = lesson.displaySubjectName?.asStrikethroughSpannable()
                                ?: ""
                    }
                    else -> {
                        card.setCardBackgroundColor(0xff234158.toInt())
                        subjectName.setTextColor(Color.WHITE)
                        subjectName.setTypeface(null, Typeface.BOLD_ITALIC)
                    }
                }

                layout.isDrawingCacheEnabled = true
                layout.measure(MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.EXACTLY))
                layout.layout(0, 0, layout.measuredWidth, layout.measuredHeight)
                layout.buildDrawingCache(true)

                val itemBitmap = layout.drawingCache
                canvas.drawBitmap(itemBitmap, null, Rect(left, top, left + blockWidth, top + blockHeight), paint)
            }

            val textPaint = Paint().apply {
                setARGB(255, 0, 0, 0)
                textAlign = Paint.Align.CENTER
                textSize = 30f
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
            }

            for (w in 0..maxWeekDay) {
                val x = WIDTH_CONSTANT + w * WIDTH_WEEKDAY + w * WIDTH_SPACING
                canvas.drawText(Week.getFullDayName(w), x + (WIDTH_WEEKDAY / 2f), heightProfileName + HEIGHT_CONSTANT / 2 + 10f, textPaint)
            }

            if (showProfileName) {
                textPaint.textSize = 50f
                canvas.drawText("${app.profile.name} - plan lekcji, ${weekStart.formattedStringShort} - ${weekEnd.formattedStringShort}", (imageWidth + 20) / 2f, 80f, textPaint)
            }

            textPaint.apply {
                setARGB(128, 0, 0, 0)
                textAlign = Paint.Align.RIGHT
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }

            val footerTextPaintCenter = ((textPaint.descent() + textPaint.ascent()) / 2).roundToInt()
            canvas.drawText("Wygenerowano w aplikacji Szkolny.eu", imageWidth - 10f, imageHeight - footerTextPaintCenter - 10f, textPaint)

            textPaint.apply {
                setARGB(255, 127, 127, 127)
                textAlign = Paint.Align.CENTER
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val textPaintCenter = ((textPaint.descent() + textPaint.ascent()) / 2).roundToInt()

            val linePaint = Paint().apply {
                setARGB(255, 100, 100, 100)
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
            }

            val minTimeInt = ((minTime!!.value / 10000) * 60) + ((minTime!!.value / 100) % 100)

            lessonRanges.forEach { (startTime, endTime) ->
                listOf(startTime, endTime).forEach { value ->
                    val hour = value / 10000
                    val minute = (value / 100) % 100
                    val time = Time(hour, minute, 0)

                    val firstOffset = time.inMinutes - minTimeInt // offset in minutes
                    val top = (heightProfileName + HEIGHT_CONSTANT + firstOffset * HEIGHT_MINUTE).toFloat()

                    canvas.drawText(time.stringHM, WIDTH_CONSTANT / 2f, top - textPaintCenter, textPaint)
                    canvas.drawLine(WIDTH_CONSTANT.toFloat(), top, imageWidth.toFloat(), top, linePaint)
                }
            }

            val today = Date.getToday().stringY_m_d
            val now = Time.getNow().stringH_M_S

            val outputDir = Environment.getExternalStoragePublicDirectory("Szkolny.eu").apply { mkdirs() }
            val outputFile = File(outputDir, "plan_lekcji_${app.profile.name}_${today}_${now}.png")

            try {
                val fos = FileOutputStream(outputFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
            } catch (e: Exception) {
                Log.e("SAVE_IMAGE", e.message, e)
                return@withContext null
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(activity, app.packageName + ".provider", outputFile)
            } else {
                Uri.parse("file://" + outputFile.absolutePath)
            }
            uri
        }

        progressDialog.dismiss()
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.timetable_generate_success_title)
                .setMessage(R.string.timetable_generate_success_text)
                .setPositiveButton(R.string.share) { dialog, _ ->
                    dialog.dismiss()

                    val intent = Intent(Intent.ACTION_SEND)
                    intent.setDataAndType(null, "image/*")
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_intent)))
                }
                .setNegativeButton(R.string.open) { dialog, _ ->
                    dialog.dismiss()

                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    activity.startActivity(intent)
                }
                .setNeutralButton(R.string.do_nothing) { dialog, _ -> dialog.dismiss() }
                .show()
    }}
}
