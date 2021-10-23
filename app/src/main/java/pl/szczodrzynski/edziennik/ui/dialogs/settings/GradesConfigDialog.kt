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

    private val profileConfig by lazy { app.config.getFor(app.profileId).grades }

    @SuppressLint("SetTextI18n")
    override suspend fun loadConfig() {
        b.customPlusCheckBox.isChecked = profileConfig.plusValue != null
        b.customPlusValue.isVisible = b.customPlusCheckBox.isChecked
        b.customMinusCheckBox.isChecked = profileConfig.minusValue != null
        b.customMinusValue.isVisible = b.customMinusCheckBox.isChecked

        b.customPlusValue.progress = profileConfig.plusValue ?: 0.5f
        b.customMinusValue.progress = profileConfig.minusValue ?: 0.25f

        when (config.orderBy) {
            ORDER_BY_DATE_DESC -> b.sortGradesByDateRadio
            ORDER_BY_SUBJECT_ASC -> b.sortGradesBySubjectRadio
            else -> null
        }?.isChecked = true

        when (profileConfig.colorMode) {
            COLOR_MODE_DEFAULT -> b.gradeColorFromERegister
            COLOR_MODE_WEIGHTED -> b.gradeColorByValue
            else -> null
        }?.isChecked = true

        when (profileConfig.yearAverageMode) {
            YEAR_ALL_GRADES -> b.gradeAverageMode4
            YEAR_1_AVG_2_AVG -> b.gradeAverageMode0
            YEAR_1_SEM_2_AVG -> b.gradeAverageMode1
            YEAR_1_AVG_2_SEM -> b.gradeAverageMode2
            YEAR_1_SEM_2_SEM -> b.gradeAverageMode3
            else -> null
        }?.isChecked = true

        b.dontCountGrades.isChecked =
            profileConfig.dontCountEnabled && profileConfig.dontCountGrades.isNotEmpty()
        b.hideImproved.isChecked = profileConfig.hideImproved
        b.averageWithoutWeight.isChecked = profileConfig.averageWithoutWeight

        if (profileConfig.dontCountGrades.isEmpty()) {
            b.dontCountGradesText.setText("nb, 0, bz, bd")
        } else {
            b.dontCountGradesText.setText(profileConfig.dontCountGrades.join(", "))
        }
    }

    override suspend fun saveConfig() {
        profileConfig.plusValue =
            if (b.customPlusCheckBox.isChecked) b.customPlusValue.progress else null
        profileConfig.minusValue =
            if (b.customMinusCheckBox.isChecked) b.customMinusValue.progress else null

        b.dontCountGradesText.setText(
            b.dontCountGradesText
                .text
                ?.toString()
                ?.lowercase()
                ?.replace(", ", ",")
        )
        profileConfig.dontCountEnabled = b.dontCountGrades.isChecked
        profileConfig.dontCountGrades = b.dontCountGradesText.text
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
            profileConfig.plusValue = it.progress
        }
        b.customMinusValue.doOnStopTrackingTouch {
            profileConfig.minusValue = it.progress
        }

        b.sortGradesByDateRadio.setOnSelectedListener { config.orderBy = ORDER_BY_DATE_DESC }
        b.sortGradesBySubjectRadio.setOnSelectedListener { config.orderBy = ORDER_BY_SUBJECT_ASC }

        b.gradeColorFromERegister.setOnSelectedListener {
            profileConfig.colorMode = COLOR_MODE_DEFAULT
        }
        b.gradeColorByValue.setOnSelectedListener { profileConfig.colorMode = COLOR_MODE_WEIGHTED }

        b.gradeAverageMode4.setOnSelectedListener {
            profileConfig.yearAverageMode = YEAR_ALL_GRADES
        }
        b.gradeAverageMode0.setOnSelectedListener {
            profileConfig.yearAverageMode = YEAR_1_AVG_2_AVG
        }
        b.gradeAverageMode1.setOnSelectedListener {
            profileConfig.yearAverageMode = YEAR_1_SEM_2_AVG
        }
        b.gradeAverageMode2.setOnSelectedListener {
            profileConfig.yearAverageMode = YEAR_1_AVG_2_SEM
        }
        b.gradeAverageMode3.setOnSelectedListener {
            profileConfig.yearAverageMode = YEAR_1_SEM_2_SEM
        }

        b.hideImproved.onChange { _, isChecked -> profileConfig.hideImproved = isChecked }
        b.averageWithoutWeight.onChange { _, isChecked ->
            profileConfig.averageWithoutWeight = isChecked
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
