/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-19.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog

class ServerMessageDialog(
    activity: AppCompatActivity,
    private val titleText: String,
    private val messageText: CharSequence,
) : BaseDialog<Any>(activity) {

    override fun getTitle() = titleText
    override fun getMessage() = messageText
    override fun getPositiveButtonText() = R.string.close
}
