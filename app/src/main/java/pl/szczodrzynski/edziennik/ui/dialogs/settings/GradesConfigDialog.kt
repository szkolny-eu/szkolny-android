/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-16
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.config.ConfigGrades
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.DialogConfigGradesBinding
import pl.szczodrzynski.edziennik.setOnSelectedListener

class GradesConfigDialog(
        val activity: AppCompatActivity,
        private val reloadOnDismiss: Boolean = true,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) {
    companion object {
        const val TAG = "GradesConfigDialog"
    }

    private val app by lazy { activity.application as App }
    private val config by lazy { app.config.grades }
    private val profileConfig by lazy { app.config.getFor(app.profileId).grades }

    private lateinit var b: DialogConfigGradesBinding
    private lateinit var dialog: AlertDialog

    init { run {
        if (activity.isFinishing)
            return@run
        b = DialogConfigGradesBinding.inflate(activity.layoutInflater)
        onShowListener?.invoke(TAG)
        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.menu_grades_config)
                .setView(b.root)
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                    if (reloadOnDismiss) (activity as? MainActivity)?.reloadTarget()
                }
                .create()
        initView()
        loadConfig()
        dialog.show()
    }}

    private fun loadConfig() {
        when (config.orderBy) {
            ConfigGrades.ORDER_BY_DATE_DESC -> b.sortGradesByDateRadio
            ConfigGrades.ORDER_BY_SUBJECT_ASC -> b.sortGradesBySubjectRadio
            else -> null
        }?.isChecked = true
        
        when (profileConfig.colorMode) {
            Profile.COLOR_MODE_DEFAULT -> b.gradeColorFromERegister
            Profile.COLOR_MODE_WEIGHTED -> b.gradeColorByValue
            else -> null
        }?.isChecked = true

        when (profileConfig.yearAverageMode) {
            Profile.YEAR_ALL_GRADES -> b.gradeAverageMode4
            Profile.YEAR_1_AVG_2_AVG -> b.gradeAverageMode0
            Profile.YEAR_1_SEM_2_AVG -> b.gradeAverageMode1
            Profile.YEAR_1_AVG_2_SEM -> b.gradeAverageMode2
            Profile.YEAR_1_SEM_2_SEM -> b.gradeAverageMode3
            else -> null
        }?.isChecked = true

        b.dontCountZeroToAverage.isChecked = !profileConfig.countZeroToAvg
    }

    private fun initView() {
        b.sortGradesByDateRadio.setOnSelectedListener { config.orderBy = ConfigGrades.ORDER_BY_DATE_DESC }
        b.sortGradesBySubjectRadio.setOnSelectedListener { config.orderBy = ConfigGrades.ORDER_BY_SUBJECT_ASC }

        b.gradeColorFromERegister.setOnSelectedListener { profileConfig.colorMode = Profile.COLOR_MODE_DEFAULT }
        b.gradeColorByValue.setOnSelectedListener { profileConfig.colorMode = Profile.COLOR_MODE_WEIGHTED }

        b.gradeAverageMode4.setOnSelectedListener { profileConfig.yearAverageMode = Profile.YEAR_ALL_GRADES }
        b.gradeAverageMode0.setOnSelectedListener { profileConfig.yearAverageMode = Profile.YEAR_1_AVG_2_AVG }
        b.gradeAverageMode1.setOnSelectedListener { profileConfig.yearAverageMode = Profile.YEAR_1_SEM_2_AVG }
        b.gradeAverageMode2.setOnSelectedListener { profileConfig.yearAverageMode = Profile.YEAR_1_AVG_2_SEM }
        b.gradeAverageMode3.setOnSelectedListener { profileConfig.yearAverageMode = Profile.YEAR_1_SEM_2_SEM }

        b.dontCountZeroToAverage.setOnCheckedChangeListener { _, isChecked -> profileConfig.countZeroToAvg = !isChecked }
    }
}
