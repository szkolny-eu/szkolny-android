/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-1.
 */

package pl.szczodrzynski.edziennik.ui.modules.grades.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.GradesItemEmptyBinding
import pl.szczodrzynski.edziennik.ui.modules.grades.models.GradesEmpty

class EmptyViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: GradesItemEmptyBinding = GradesItemEmptyBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<GradesEmpty> {
    companion object {
        private const val TAG = "EmptyViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: GradesEmpty, position: Int) {

    }
}
