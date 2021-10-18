/*
 * Copyright (c) Antoni Czaplicki 2021-10-15.
 */

package pl.szczodrzynski.edziennik.ui.modules.teachers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.databinding.TeacherItemBinding
import pl.szczodrzynski.edziennik.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesUtils.getProfileImage
import pl.szczodrzynski.edziennik.utils.BetterLink
import kotlin.coroutines.CoroutineContext

class TeachersAdapter(
    private val activity: AppCompatActivity,
    val onItemClick: ((item: Teacher) -> Unit)? = null,
) : RecyclerView.Adapter<TeachersAdapter.ViewHolder>(), CoroutineScope {
    companion object {
        private const val TAG = "TeachersAdapter"
    }

    private val app = activity.applicationContext as App
    // optional: place the manager here

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = listOf<Teacher>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(activity)
        val view = TeacherItemBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.b

        b.name.text = item.fullName
        b.image.setImageBitmap(item.image?: getProfileImage(48, 24, 16, 12, 1, item.fullName))
        var role = item.getTypeText(activity)
        if (item.subjects.isNotNullNorEmpty()) {
            val subjects = item.subjects.map { App.db.subjectDao().getByIdNow(App.profileId, it).longName }
            role = role.plus(": ").plus(subjects.joinToString())
        }
        b.type.text = role

        item.fullName.let { name ->
            BetterLink.attach(
                b.name,
                teachers = mapOf(item.id to name)
            )
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: TeacherItemBinding) : RecyclerView.ViewHolder(b.root)
}
