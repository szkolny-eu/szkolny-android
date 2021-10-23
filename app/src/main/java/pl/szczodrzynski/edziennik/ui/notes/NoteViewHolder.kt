/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-23.
 */

package pl.szczodrzynski.edziennik.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.databinding.NoteListItemBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.utils.models.Date

class NoteViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: NoteListItemBinding = NoteListItemBinding.inflate(inflater, parent, false),
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<Note, NoteListAdapter> {
    companion object {
        private const val TAG = "NoteViewHolder"
    }

    override fun onBind(
        activity: AppCompatActivity,
        app: App,
        item: Note,
        position: Int,
        adapter: NoteListAdapter,
    ) {
        b.topic.text = item.topicHtml ?: item.bodyHtml

        val colorHighlight = R.attr.colorControlHighlight.resolveAttr(activity)
        val addedDate = Date.fromMillis(item.addedDate).formattedString

        if (item.sharedBy != null && item.sharedByName != null) {
            b.addedBy.text = listOf<CharSequence>(
                "{cmd-share-variant}",
                item.sharedByName,
                "•",
                addedDate,
            ).concat(" ")

            // workaround for the span data lost during setText above
            val sharedBySpanned = adapter.highlightSearchText(
                item = item,
                text = item.sharedByName,
                color = colorHighlight
            )
            b.addedBy.text = b.addedBy.text.replaceSpanned(item.sharedByName, sharedBySpanned)
        } else {
            b.addedBy.setText(R.string.notes_added_by_you_format, addedDate)
        }

        b.editButton.isVisible = item.sharedBy == "self" && adapter.onNoteEditClick != null

        if (adapter.onNoteClick != null)
            b.root.onClick {
                adapter.onNoteClick.invoke(item)
            }
        if (adapter.onNoteEditClick != null)
            b.editButton.onClick {
                adapter.onNoteEditClick.invoke(item)
            }
    }
}
