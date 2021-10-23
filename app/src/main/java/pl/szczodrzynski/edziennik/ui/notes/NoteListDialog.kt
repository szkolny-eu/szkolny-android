/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-23.
 */

package pl.szczodrzynski.edziennik.ui.notes

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.databinding.NoteListDialogBinding
import pl.szczodrzynski.edziennik.ui.dialogs.base.BindingDialog

class NoteListDialog(
    activity: AppCompatActivity,
    private val profileId: Int,
    private val owner: Noteable,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<NoteListDialogBinding>(activity, onShowListener, onDismissListener) {

    override val TAG = "NoteListDialog"

    override fun getTitleRes() = R.string.notes_list_dialog_title
    override fun inflate(layoutInflater: LayoutInflater) =
        NoteListDialogBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.cancel
    override fun getNeutralButtonText() = R.string.add

    private val manager
        get() = app.noteManager

    override suspend fun onNeutralClick(): Boolean {
        return NO_DISMISS
    }

    override suspend fun onShow() {
        b.ownerItemList.apply {
            adapter = manager.getAdapterForItem(activity, owner)
            isNestedScrollingEnabled = false
            //setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }
    }
}
