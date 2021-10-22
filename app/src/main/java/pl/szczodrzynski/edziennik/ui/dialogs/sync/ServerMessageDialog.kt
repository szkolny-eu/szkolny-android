/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-19.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog

class ServerMessageDialog(
    activity: AppCompatActivity,
    private val titleText: String,
    private val messageText: CharSequence,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog(activity, onShowListener, onDismissListener) {

    override val TAG = "ServerMessageDialog"

    override fun getTitle() = titleText
    override fun getTitleRes(): Int? = null
    override fun getMessage() = messageText
    override fun getPositiveButtonText() = R.string.close

    override suspend fun onShow() = Unit
}
