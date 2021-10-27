/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-27.
 */

package pl.szczodrzynski.edziennik.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.databinding.NotesFragmentBinding
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class NotesFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "NotesFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: NotesFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val manager
        get() = app.noteManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        activity = getActivity() as? MainActivity ?: return null
        context ?: return null
        app = activity.application as App
        b = NotesFragmentBinding.inflate(inflater)
        return b.root
    }

    private fun onNoteClick(note: Note) = launch {
        val owner = withContext(Dispatchers.IO) {
            manager.getOwner(note)
        } as? Noteable ?: return@launch

        NoteDetailsDialog(
            activity = activity,
            owner = owner,
            note = note,
        ).show()
    }

    private fun onNoteEditClick(note: Note) = launch {
        val owner = withContext(Dispatchers.IO) {
            manager.getOwner(note)
        } as? Noteable ?: return@launch

        NoteEditorDialog(
            activity = activity,
            owner = owner,
            editingNote = note,
        ).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

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
}
