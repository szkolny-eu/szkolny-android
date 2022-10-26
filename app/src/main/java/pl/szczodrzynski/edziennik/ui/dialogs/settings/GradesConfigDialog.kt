/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-16
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.sephiroth.android.library.numberpicker.doOnStopTrackingTouch
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogConfigGradesBinding
import pl.szczodrzynski.edziennik.ext.join
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.setOnSelectedListener
import pl.szczodrzynski.edziennik.ui.dialogs.base.ConfigDialog
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.COLOR_MODE_DEFAULT
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.COLOR_MODE_WEIGHTED
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.ORDER_BY_DATE_DESC
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.ORDER_BY_SUBJECT_ASC
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_1_AVG_2_AVG
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_1_AVG_2_SEM
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_1_SEM_2_AVG
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_1_SEM_2_SEM
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_ALL_GRADES

class GradesConfigDialog(
    activity: AppCompatActivity,
    reloadOnDismiss: Boolean = true,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : ConfigDialog<DialogConfigGradesBinding>(
    activity,
    reloadOnDismiss,
    onShowListener,
    onDismissListener,
) {

    override val TAG = "GradesConfigDialog"

    override fun getTitleRes() = R.string.menu_grades_config
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogConfigGradesBinding.inflate(layoutInflater)

    @SuppressLint("SetTextI18n")
    override suspend fun loadConfig() {
        b.customPlusCheckBox.isChecked = app.profile.config.grades.plusValue != null
        b.customPlusValue.isVisible = b.customPlusCheckBox.isChecked
        b.customMinusCheckBox.isChecked = app.profile.config.grades.minusValue != null
        b.customMinusValue.isVisible = b.customMinusCheckBox.isChecked

        b.customPlusValue.progress = app.profile.config.grades.plusValue ?: 0.5f
        b.customMinusValue.progress = app.profile.config.grades.minusValue ?: 0.25f

        when (config.orderBy) {
            ORDER_BY_DATE_DESC -> b.sortGradesByDateRadio
            ORDER_BY_SUBJECT_ASC -> b.sortGradesBySubjectRadio
            else -> null
        }?.isChecked = true

        when (app.profile.config.grades.colorMode) {
            COLOR_MODE_DEFAULT -> b.gradeColorFromERegister
            COLOR_MODE_WEIGHTED -> b.gradeColorByValue
            else -> null
        }?.isChecked = true

        when (app.profile.config.grades.yearAverageMode) {
            YEAR_ALL_GRADES -> b.gradeAverageMode4
            YEAR_1_AVG_2_AVG -> b.gradeAverageMode0
            YEAR_1_SEM_2_AVG -> b.gradeAverageMode1
            YEAR_1_AVG_2_SEM -> b.gradeAverageMode2
            YEAR_1_SEM_2_SEM -> b.gradeAverageMode3
            else -> null
        }?.isChecked = true

        b.dontCountGrades.isChecked =
            app.profile.config.grades.dontCountEnabled && app.profile.config.grades.dontCountGrades.isNotEmpty()
        b.hideImproved.isChecked = app.profile.config.grades.hideImproved
        b.averageWithoutWeight.isChecked = app.profile.config.grades.averageWithoutWeight

        if (app.profile.config.grades.dontCountGrades.isEmpty()) {
            b.dontCountGradesText.setText("nb, 0, bz, bd")
        } else {
            b.dontCountGradesText.setText(app.profile.config.grades.dontCountGrades.join(", "))
        }
    }

    override suspend fun saveConfig() {
        app.profile.config.grades.plusValue =
            if (b.customPlusCheckBox.isChecked) b.customPlusValue.progress else null
        app.profile.config.grades.minusValue =
            if (b.customMinusCheckBox.isChecked) b.customMinusValue.progress else null

        b.dontCountGradesText.setText(
            b.dontCountGradesText
                .text
                ?.toString()
                ?.lowercase()
                ?.replace(", ", ",")
        )
        app.profile.config.grades.dontCountEnabled = b.dontCountGrades.isChecked
        app.profile.config.grades.dontCountGrades = b.dontCountGradesText.text
            ?.split(",")
            ?.map { it.trim() }
            ?: listOf()
    }

    override fun initView() {
        b.customPlusCheckBox.onChange { _, isChecked ->
            b.customPlusValue.isVisible = isChecked
        }
        b.customMinusCheckBox.onChange { _, isChecked ->
            b.customMinusValue.isVisible = isChecked
        }

        // who the hell named those methods
        // THIS SHIT DOES NOT EVEN WORK
        b.customPlusValue.doOnStopTrackingTouch {
            app.profile.config.grades.plusValue = it.progress
        }
        b.customMinusValue.doOnStopTrackingTouch {
            app.profile.config.grades.minusValue = it.progress
        }

        b.sortGradesByDateRadio.setOnSelectedListener { config.orderBy = ORDER_BY_DATE_DESC }
        b.sortGradesBySubjectRadio.setOnSelectedListener { config.orderBy = ORDER_BY_SUBJECT_ASC }

        b.gradeColorFromERegister.setOnSelectedListener {
            app.profile.config.grades.colorMode = COLOR_MODE_DEFAULT
        }
        b.gradeColorByValue.setOnSelectedListener { app.profile.config.grades.colorMode = COLOR_MODE_WEIGHTED }

        b.gradeAverageMode4.setOnSelectedListener {
            app.profile.config.grades.yearAverageMode = YEAR_ALL_GRADES
        }
        b.gradeAverageMode0.setOnSelectedListener {
            app.profile.config.grades.yearAverageMode = YEAR_1_AVG_2_AVG
        }
        b.gradeAverageMode1.setOnSelectedListener {
            app.profile.config.grades.yearAverageMode = YEAR_1_SEM_2_AVG
        }
        b.gradeAverageMode2.setOnSelectedListener {
            app.profile.config.grades.yearAverageMode = YEAR_1_AVG_2_SEM
        }
        b.gradeAverageMode3.setOnSelectedListener {
            app.profile.config.grades.yearAverageMode = YEAR_1_SEM_2_SEM
        }

        b.hideImproved.onChange { _, isChecked -> app.profile.config.grades.hideImproved = isChecked }
        b.averageWithoutWeight.onChange { _, isChecked ->
            app.profile.config.grades.averageWithoutWeight = isChecked
        }

        b.averageWithoutWeightHelp.onClick {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.grades_config_average_without_weight)
                .setMessage(R.string.grades_config_average_without_weight_message)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }
}
