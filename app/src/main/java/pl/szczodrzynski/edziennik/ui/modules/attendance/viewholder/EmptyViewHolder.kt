/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-4.
 */

package pl.szczodrzynski.edziennik.ui.modules.attendance.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.AttendanceItemEmptyBinding
import pl.szczodrzynski.edziennik.ui.modules.attendance.AttendanceAdapter
import pl.szczodrzynski.edziennik.ui.modules.attendance.models.AttendanceEmpty
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder

class EmptyViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: AttendanceItemEmptyBinding = AttendanceItemEmptyBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<AttendanceEmpty, AttendanceAdapter> {
    companion object {
        private const val TAG = "EmptyViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: AttendanceEmpty, position: Int, adapter: AttendanceAdapter) {

    }
}
