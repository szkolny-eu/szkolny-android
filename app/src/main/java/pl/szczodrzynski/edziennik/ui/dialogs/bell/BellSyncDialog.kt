/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-20
 */

package pl.szczodrzynski.edziennik.ui.dialogs.bell

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogBellSyncBinding
import pl.szczodrzynski.edziennik.startCoroutineTimer
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.coroutines.CoroutineContext

class BellSyncDialog(
        val activity: AppCompatActivity,
        private val bellTime: Time,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {

    companion object {
        const val TAG = "BellSyncDialog"
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var dialog: AlertDialog
    private lateinit var b: DialogBellSyncBinding

    private val app by lazy { activity.application as App }

    private var counterJob: Job? = null

    private val actualBellDiff: Pair<Time, Int>
        get() {
            val now = Time.getNow()
            val bellDiff = Time.diff(now, bellTime)
            val multiplier = if (bellTime > now) -1 else 1
            return Pair(bellDiff, multiplier)
        }

    init { apply {
        if (activity.isFinishing)
            return@apply
        job = Job()
        b = DialogBellSyncBinding.inflate(activity.layoutInflater)
        onShowListener?.invoke(TAG)
        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.bell_sync_title)
                .setView(b.root)
                .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener {
                    counterJob?.cancel()
                    onDismissListener?.invoke(TAG)
                }
                .show()
        initView()
    }}

    private fun initView() {
        b.bellSyncButton.setOnClickListener {
            val (bellDiff, multiplier) = actualBellDiff
            val bellDiffText = (if (multiplier == -1) '-' else '+') + bellDiff.stringHMS
            app.config.timetable.bellSyncDiff = bellDiff
            app.config.timetable.bellSyncMultiplier = multiplier

            MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.bell_sync_title)
                    .setMessage(app.getString(R.string.bell_sync_results, bellDiffText))
                    .setPositiveButton(R.string.ok) { resultsDialog, _ ->
                        resultsDialog.dismiss()
                        dialog.dismiss()
                        if (activity is MainActivity) activity.reloadTarget()
                    }
                    .show()
        }

        if (Time.diff(Time.getNow(), bellTime) > Time(2, 0, 0)) { // Easter egg ^^
            b.bellSyncButton.setImageDrawable(app.resources.getDrawable(R.drawable.ic_bell_wtf)) // wtf
        }

        launch {
            counterJob = startCoroutineTimer(repeatMillis = 1000) {
                val (bellDiff, multiplier) = actualBellDiff
                val bellDiffText = (if (multiplier == -1) '-' else '+') + bellDiff.stringHMS
                b.bellSyncHowto.text = app.getString(R.string.bell_sync_howto, bellTime.stringHM, bellDiffText)
            }
        }
    }
}
