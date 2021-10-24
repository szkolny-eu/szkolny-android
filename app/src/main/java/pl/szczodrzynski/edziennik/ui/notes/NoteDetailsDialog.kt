/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-24.
 */

package pl.szczodrzynski.edziennik.ui.notes

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.databinding.NoteDetailsDialogBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.dialogs.base.BindingDialog
import pl.szczodrzynski.edziennik.utils.models.Date

class NoteDetailsDialog(
    activity: AppCompatActivity,
    private val owner: Noteable,
    private var note: Note,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<NoteDetailsDialogBinding>(activity, onShowListener, onDismissListener) {

    override val TAG = "NoteDetailsDialog"

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        NoteDetailsDialogBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close
    override fun getNeutralButtonText() = if (note.canEdit) R.string.homework_edit else null

    private val manager
        get() = app.noteManager

    override suspend fun onNeutralClick(): Boolean {
        NoteEditorDialog(
            activity = activity,
            owner = owner,
            editingNote = note,
            onShowListener = onShowListener,
            onDismissListener = onDismissListener,
        ).show()
        return NO_DISMISS
    }

    override suspend fun onShow() {
        manager.configureHeader(activity, owner, b.header)

        b.idsLayout.isVisible = App.devMode

        // watch the note for changes
        app.db.noteDao().get(note.profileId, note.id).observe(activity) {
            note = it ?: return@observe
            update()
        }
    }

    private fun update() {
        b.note = note

        if (note.color != null) {
            dialog.overlayBackgroundColor(note.color!!.toInt(), 0x50)
        } else {
            dialog.overlayBackgroundColor(0, 0)
        }

        b.addedBy.setText(
            when (note.sharedBy) {
                null -> R.string.notes_added_by_you_format
                "self" -> R.string.event_details_shared_by_self_format
                else -> R.string.event_details_shared_by_format
            },
            Date.fromMillis(note.addedDate).formattedString,
            note.sharedByName ?: "",
        )
    }
}
