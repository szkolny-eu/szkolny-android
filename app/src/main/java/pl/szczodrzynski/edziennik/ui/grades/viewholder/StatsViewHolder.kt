/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-3.
 */

package pl.szczodrzynski.edziennik.ui.grades.viewholder

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.GradesItemStatsBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.dialogs.settings.GradesConfigDialog
import pl.szczodrzynski.edziennik.ui.grades.GradesAdapter
import pl.szczodrzynski.edziennik.ui.grades.models.GradesStats
import java.text.DecimalFormat

class StatsViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: GradesItemStatsBinding = GradesItemStatsBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<GradesStats, GradesAdapter> {
    companion object {
        private const val TAG = "StatsViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: GradesStats, position: Int, adapter: GradesAdapter) {
        val manager = app.gradesManager
        val showAverages = mutableListOf<Int>()
        val showPoint = mutableListOf<Int>()

        getSemesterString(app, item.normalSem1, item.normalSem1Proposed, item.normalSem1Final, item.sem1NotAllFinal).let { (average, notice) ->
            b.normalSemester1Layout.isVisible = average != null
            b.normalSemester1Notice.isVisible = notice != null
            b.normalSemester1.text = average
            b.normalSemester1Notice.text = notice
            if (average != null)
                showAverages += 1
        }
        getSemesterString(app, item.normalSem2, item.normalSem2Proposed, item.normalSem2Final, item.sem2NotAllFinal).let { (average, notice) ->
            b.normalSemester2Layout.isVisible = average != null
            b.normalSemester2Notice.isVisible = notice != null
            b.normalSemester2.text = average
            b.normalSemester2Notice.text = notice
            if (average != null)
                showAverages += 2
        }
        getSemesterString(app, item.normalYearly, item.normalYearlyProposed, item.normalYearlyFinal, item.yearlyNotAllFinal).let { (average, notice) ->
            b.normalYearlyLayout.isVisible = average != null
            b.normalYearlyNotice.isVisible = notice != null
            b.normalYearly.text = average
            b.normalYearlyNotice.text = notice
            if (average != null)
                showAverages += 3
        }

        b.normalTitle.isVisible = showAverages.size > 0
        b.normalLayout.isVisible = showAverages.size > 0
        b.normalDivider.isVisible = showAverages.size > 0
        b.helpButton.isVisible = showAverages.size > 0
        b.normalDiv1.isVisible = showAverages.contains(1) && showAverages.contains(2) || showAverages.contains(1) && showAverages.contains(3)
        b.normalDiv2.isVisible = showAverages.contains(2) && showAverages.contains(3)

        getSemesterString(app, 0f, 0f, item.pointSem1, false).let { (average, _) ->
            b.pointSemester1Layout.isVisible = average != null
            b.pointSemester1.text = average
            if (average != null)
                showPoint += 1
        }
        getSemesterString(app, 0f, 0f, item.pointSem2, false).let { (average, _) ->
            b.pointSemester2Layout.isVisible = average != null
            b.pointSemester2.text = average
            if (average != null)
                showPoint += 2
        }
        getSemesterString(app, 0f, 0f, item.pointYearly, false).let { (average, _) ->
            b.pointYearlyLayout.isVisible = average != null
            b.pointYearly.text = average
            if (average != null)
                showPoint += 3
        }

        b.pointTitle.isVisible = showPoint.size > 0
        b.pointLayout.isVisible = showPoint.size > 0
        b.pointDivider.isVisible = showPoint.size > 0
        b.pointDiv1.isVisible = showPoint.contains(1) && showPoint.contains(2)
        b.pointDiv2.isVisible = showPoint.contains(2) && showPoint.contains(3)

        b.noData.isVisible = showAverages.isEmpty() && showPoint.isEmpty()
        b.disclaimer.isVisible = !b.noData.isVisible

        b.helpButton.onClick {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.grades_stats_help_title)
                .setMessage(R.string.grades_stats_help_text)
                .setPositiveButton(R.string.ok, null)
                .show()
        }

        b.customValueDivider.isVisible = manager.dontCountEnabled || manager.plusValue != null || manager.minusValue != null
        b.customValueLayout.isVisible = b.customValueDivider.isVisible
        b.customValueButton.onClick {
            GradesConfigDialog(activity, reloadOnDismiss = true)
        }
    }

    private fun getSemesterString(context: Context, expected: Float, proposed: Float, final: Float, notAllFinal: Boolean) : Pair<String?, String?> {
        val format = DecimalFormat("#.00")

        val average = when {
            final != 0f -> final
            proposed != 0f -> proposed
            expected != 0f -> expected
            else -> null
        }?.let {
            format.format(it)
        }

        val notice = when {
            final != 0f -> when {
                notAllFinal -> if (expected != 0f)
                    context.getString(R.string.grades_stats_from_final, format.format(expected))
                else
                    context.getString(R.string.grades_stats_from_final_no_expected)
                proposed != 0f -> context.getString(R.string.grades_stats_proposed_avg, format.format(proposed))
                else -> null
            }
            proposed != 0f -> if (expected != 0f)
                context.getString(R.string.grades_stats_from_proposed, format.format(expected))
            else
                context.getString(R.string.grades_stats_from_proposed_no_expected)
            expected != 0f -> context.getString(R.string.grades_stats_expected)
            else -> null
        }

        return average to notice
    }
}
