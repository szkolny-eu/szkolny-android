/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-1.
 */

package pl.szczodrzynski.edziennik.ui.attendance

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.utils.Colors
import kotlin.math.roundToInt

/* https://github.com/JakubekWeg/Mobishit/blob/master/app/src/main/java/jakubweg/mobishit/view/AttendanceBarView.kt */
class AttendanceBar : View {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private var attendancesList = listOf<AttendanceItem>()
    private val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).also {
        it.textAlign = Paint.Align.CENTER
    }
    private var mPath = Path()
    private var mCornerRadius: Float = 0.toFloat()

    init {
        mCornerRadius = 4.dp.toFloat()

        if (isInEditMode)
            setAttendanceData(listOf(
                    0xff43a047.toInt() to 23,
                    0xff009688.toInt() to 187,
                    0xff3f51b5.toInt() to 46,
                    0xff3f51b5.toInt() to 5,
                    0xffffc107.toInt() to 5,
                    0xff9e9e9e.toInt() to 26,
                    0xff76ff03.toInt() to 34,
                    0xffff3d00.toInt() to 8
            ))
    }

    // color, count
    private class AttendanceItem(var color: Int, var count: Int)

    fun setAttendanceData(list: List<Pair<Int, Int>>) {
        attendancesList = list.map { AttendanceItem(it.first, it.second) }
        setWillNotDraw(false)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val r = RectF(0f, 0f, w.toFloat(), h.toFloat())
        mPath = Path().apply {
            addRoundRect(r, mCornerRadius, mCornerRadius, Path.Direction.CW)
            close()
        }
    }

    @SuppressLint("DrawAllocation", "CanvasSize")
    override fun onDraw(canvas: Canvas?) {
        canvas ?: return

        val sum = attendancesList.sumOf { it.count }
        if (sum == 0) {
            return
        }

        canvas.clipPath(mPath)

        val top = paddingTop.toFloat()
        val bottom = (height - paddingBottom).toFloat()
        var left = paddingLeft.toFloat()
        val unitWidth = (width - paddingRight - paddingLeft).toFloat() / sum.toFloat()

        textPaint.color = Color.BLACK
        textPaint.textSize = 14.dp.toFloat()

        for (e in attendancesList) {
            if (e.count == 0)
                continue

            val width = unitWidth * e.count
            mainPaint.color = e.color
            canvas.drawRect(left, top, left + width, bottom, mainPaint)

            val percentage = (100f * e.count / sum).roundToInt().toString() + "%"
            val textBounds = Rect()
            textPaint.getTextBounds(percentage, 0, percentage.length, textBounds)
            if (width > textBounds.width() + 8.dp && height > textBounds.height() + 2.dp) {
                textPaint.color = Colors.legibleTextColor(e.color)
                canvas.drawText(percentage, left + width / 2, bottom - height / 2 + textBounds.height()/2, textPaint)
            }

            left += width
        }
    }
}
