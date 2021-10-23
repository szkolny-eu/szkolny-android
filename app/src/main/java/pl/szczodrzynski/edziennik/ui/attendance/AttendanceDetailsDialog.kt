/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-9.
 */

package pl.szczodrzynski.edziennik.ui.attendance

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.databinding.AttendanceDetailsDialogBinding
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.ui.dialogs.base.BindingDialog
import pl.szczodrzynski.edziennik.utils.BetterLink

class AttendanceDetailsDialog(
    activity: AppCompatActivity,
    private val attendance: AttendanceFull,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<AttendanceDetailsDialogBinding>(activity, onShowListener, onDismissListener) {

    override val TAG = "AttendanceDetailsDialog"

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        AttendanceDetailsDialogBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close

    override suspend fun onShow() {
        val manager = app.attendanceManager

        val attendanceColor = manager.getAttendanceColor(attendance)
        b.attendance = attendance
        b.devMode = App.devMode
        b.attendanceName.setTextColor(if (ColorUtils.calculateLuminance(attendanceColor) > 0.3) 0xaa000000.toInt() else 0xccffffff.toInt())
        b.attendanceName.background.setTintColor(attendanceColor)

        b.attendanceIsCounted.setText(if (attendance.isCounted) R.string.yes else R.string.no)

        attendance.teacherName?.let { name ->
            BetterLink.attach(
                b.teacherName,
                teachers = mapOf(attendance.teacherId to name),
                onActionSelected = dialog::dismiss
            )
        }
    }
}
