/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-23.
 */

package pl.szczodrzynski.edziennik.ui.notes

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.databinding.NoteListDialogBinding
import pl.szczodrzynski.edziennik.ui.dialogs.base.BindingDialog
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration

class NoteListDialog(
    activity: AppCompatActivity,
    private val owner: Noteable,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<NoteListDialogBinding>(activity, onShowListener, onDismissListener) {

    override val TAG = "NoteListDialog"

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        NoteListDialogBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close
    override fun getNeutralButtonText() = R.string.add

    private val manager
        get() = app.noteManager

    private lateinit var adapter: NoteListAdapter

    override suspend fun onNeutralClick(): Boolean {
        NoteEditorDialog(
            activity = activity,
            owner = owner,
            editingNote = null,
            onShowListener = onShowListener,
            onDismissListener = onDismissListener,
        ).show()
        return NO_DISMISS
    }

    override suspend fun onShow() {
        manager.configureHeader(activity, owner, b.header)

        adapter = NoteListAdapter(
            activity = activity,
            onNoteClick = {
                NoteDetailsDialog(
                    activity = activity,
                    owner = owner,
                    note = it,
                    onShowListener = onShowListener,
                    onDismissListener = onDismissListener,
                ).show()
            },
            onNoteEditClick = {
                NoteEditorDialog(
                    activity = activity,
                    owner = owner,
                    editingNote = it,
                    onShowListener = onShowListener,
                    onDismissListener = onDismissListener,
                ).show()
            },
        )

        app.db.noteDao().getAllFor(
            profileId = owner.getNoteOwnerProfileId(),
            ownerType = owner.getNoteType(),
            ownerId = owner.getNoteOwnerId()
        ).observe(activity) { notes ->

            // show/hide relevant views
            b.noteListLayout.isVisible = notes.isNotEmpty()
            b.noData.isVisible = notes.isEmpty()
            if (notes.isEmpty()) {
                return@observe
            }

            // apply the new note list
            adapter.setAllItems(notes)

            // configure the adapter & recycler view
            if (b.noteList.adapter == null) {
                b.noteList.adapter = adapter
                b.noteList.apply {
                    //setHasFixedSize(true)
                    isNestedScrollingEnabled = false
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(SimpleDividerItemDecoration(context))
                }
            } else {
                adapter.notifyDataSetChanged()
            }
        }
    }
}
