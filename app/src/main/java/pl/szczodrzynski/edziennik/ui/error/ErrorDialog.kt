/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-13.
 */

package pl.szczodrzynski.edziennik.ui.error

import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogLessonDetailsBinding
import kotlin.coroutines.CoroutineContext

class ErrorDialog(
        val activity: AppCompatActivity,
        val exception: Exception
) : CoroutineScope {
    companion object {
        private const val TAG = "ErrorDialog"
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val app by lazy { activity.application as App }
    private lateinit var b: DialogLessonDetailsBinding
    private lateinit var dialog: AlertDialog

    init { run {
        job = Job()

        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.error_occured)
                .setMessage(exception.message + "\n" + Log.getStackTraceString(exception))
                .setPositiveButton(R.string.ok) { _, _ ->
                    dialog.dismiss()
                }
                .show()
    }}
}
