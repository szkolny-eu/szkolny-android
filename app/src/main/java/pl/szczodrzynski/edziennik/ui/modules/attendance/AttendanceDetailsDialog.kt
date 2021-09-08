/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-9.
 */

package pl.szczodrzynski.edziennik.ui.modules.attendance

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.databinding.AttendanceDetailsDialogBinding
import pl.szczodrzynski.edziennik.setTintColor
import pl.szczodrzynski.edziennik.utils.BetterLink
import kotlin.coroutines.CoroutineContext

class AttendanceDetailsDialog(
        val activity: AppCompatActivity,
        val attendance: AttendanceFull,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "AttendanceDetailsDialog"
    }

    private lateinit var app: App
    private lateinit var b: AttendanceDetailsDialogBinding
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local variables go here

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        b = AttendanceDetailsDialogBinding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setView(b.root)
                .setPositiveButton(R.string.close, null)
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()
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
    }}
}
