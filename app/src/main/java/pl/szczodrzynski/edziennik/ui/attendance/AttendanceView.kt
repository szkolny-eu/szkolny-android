/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-29.
 */

package pl.szczodrzynski.edziennik.ui.attendance

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.ColorUtils
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.dp
import pl.szczodrzynski.edziennik.setTintColor
import pl.szczodrzynski.edziennik.utils.managers.AttendanceManager

class AttendanceView : AppCompatTextView {

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attendance: Attendance, manager: AttendanceManager) : this(context, null) {
        setAttendance(attendance, manager, false)
    }

    @SuppressLint("RestrictedApi")
    fun setAttendance(attendance: Attendance?, manager: AttendanceManager, bigView: Boolean = false) {
        if (attendance == null) {
            visibility = View.GONE
            return
        }
        visibility = View.VISIBLE

        val attendanceName = if (manager.useSymbols)
            attendance.typeSymbol
        else
            attendance.typeShort

        val attendanceColor = manager.getAttendanceColor(attendance)

        text = when {
            attendanceName.isBlank() -> "  "
            else -> attendanceName
        }

        setTextColor(if (ColorUtils.calculateLuminance(attendanceColor) > 0.3)
            0xaa000000.toInt()
        else
            0xccffffff.toInt())

        setBackgroundResource(if (bigView) R.drawable.bg_rounded_8dp else R.drawable.bg_rounded_4dp)
        background.setTintColor(attendanceColor)
        gravity = Gravity.CENTER

        if (bigView) {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setAutoSizeTextTypeUniformWithConfiguration(
                    14,
                    32,
                    1,
                    TypedValue.COMPLEX_UNIT_SP
            )
            setPadding(2.dp, 2.dp, 2.dp, 2.dp)
        }
        else {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(5.dp, 0, 5.dp, 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 5.dp, 0)
            }
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}

