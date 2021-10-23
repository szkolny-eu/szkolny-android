/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-23.
 */

package pl.szczodrzynski.edziennik.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.ui.search.SearchableAdapter

class NoteListAdapter(
    val activity: AppCompatActivity,
    val onNoteClick: ((note: Note) -> Unit)? = null,
    val onNoteEditClick: ((note: Note) -> Unit)? = null,
) : SearchableAdapter<Note>() {
    companion object {
        private const val TAG = "NoteListAdapter"
        private const val ITEM_TYPE_NOTE = 0
    }

    private val app = activity.applicationContext as App

    override fun getItemViewType(item: Note) = ITEM_TYPE_NOTE

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        item: Note,
    ) {
        if (holder !is NoteViewHolder)
            return
        holder.onBind(activity, app, item, position, this)
    }

    override fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int,
    ) = NoteViewHolder(inflater, parent)
}
