/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-24.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ext.takeValue
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog

class StudentNumberDialog(
    activity: AppCompatActivity,
    val profile: Profile,
) : BaseDialog<Unit>(activity) {

    override fun getTitleRes() = R.string.card_lucky_number_set_title
    override fun getMessageRes() = R.string.card_lucky_number_set_text
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getInputType() = InputType.TYPE_CLASS_NUMBER
    override fun getInputValue() = profile.studentNumber.takeValue()?.toString()

    override suspend fun onPositiveClick(): Boolean {
        profile.studentNumber = getInput()?.text?.toString()?.toIntOrNull() ?: -1
        app.profileSave(profile)
        return DISMISS
    }
}
