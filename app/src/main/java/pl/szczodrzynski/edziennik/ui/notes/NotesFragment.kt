/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-27.
 */

package pl.szczodrzynski.edziennik.ui.notes

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.databinding.NotesFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration

class NotesFragment : BaseFragment<NotesFragmentBinding, MainActivity>(
    inflater = NotesFragmentBinding::inflate,
) {

    override fun getFab() =
        R.string.notes_action_add to CommunityMaterial.Icon3.cmd_text_box_plus_outline

    private val manager
        get() = app.noteManager

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        val adapter = NoteListAdapter(
            activity = activity,
            onNoteClick = this::onNoteClick,
            onNoteEditClick = this::onNoteEditClick,
        )

        app.db.noteDao().getAll(profileId = App.profileId).observe(activity) { allNotes ->
            if (!isAdded) return@observe

            // show/hide relevant views
            b.progressBar.isVisible = false
            b.list.isVisible = allNotes.isNotEmpty()
            b.noData.isVisible = allNotes.isEmpty()
            if (allNotes.isEmpty()) {
                return@observe
            }

            val notes = allNotes.groupBy { it.ownerType }.flatMap { (ownerType, notes) ->
                if (ownerType != null) {
                    // construct a dummy note, used as the category separator
                    val categoryItem = Note(
                        profileId = 0,
                        id = 0,
                        ownerType = ownerType,
                        ownerId = 0,
                        topic = null,
                        body = "",
                        color = null,
                    )
                    categoryItem.isCategoryItem = true
                    val mutableNotes = notes.toMutableList()
                    mutableNotes.add(0, categoryItem)
                    return@flatMap mutableNotes
                }
                return@flatMap notes
            }

            // apply the new note list
            adapter.setAllItems(notes, addSearchField = true)

            // configure the adapter & recycler view
            if (b.list.adapter == null) {
                b.list.adapter = adapter
                b.list.apply {
                    //setHasFixedSize(true)
                    isNestedScrollingEnabled = false
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(SimpleDividerItemDecoration(context))
                }
            }

            // reapply the filter
            adapter.getSearchField()?.applyTo(adapter)
        }
    }

    private fun onNoteClick(note: Note) = launch {
        val owner = withContext(Dispatchers.IO) {
            manager.getOwner(note)
        } as? Noteable

        NoteDetailsDialog(
            activity = activity,
            owner = owner,
            note = note,
        ).show()
    }

    private fun onNoteEditClick(note: Note) = launch {
        val owner = withContext(Dispatchers.IO) {
            manager.getOwner(note)
        } as? Noteable

        NoteEditorDialog(
            activity = activity,
            owner = owner,
            editingNote = note,
            profileId = App.profileId,
        ).show()
    }

    override suspend fun onFabClick() {
        NoteEditorDialog(
            activity = activity,
            owner = null,
            editingNote = null,
            profileId = App.profileId,
        ).show()
    }
}
