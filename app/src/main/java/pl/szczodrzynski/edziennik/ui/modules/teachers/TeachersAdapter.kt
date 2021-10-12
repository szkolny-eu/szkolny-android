package pl.szczodrzynski.edziennik.ui.modules.teachers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.colorRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.databinding.TeachersListItemBinding
import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.coroutines.CoroutineContext

class TeachersAdapter(
        private val activity: AppCompatActivity,
        val onItemClick: ((item: Teacher) -> Unit)? = null
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
        val view = TeachersListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.b

        val colorSecondary = android.R.attr.textColorSecondary.resolveAttr(activity)


        b.name.text = item.fullName
        b.initials.text = item.initialsLastFirst
        b.role.text = item.shortName

        onItemClick?.let { listener ->
            b.root.onClick { listener(item) }
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: TeachersListItemBinding) : RecyclerView.ViewHolder(b.root)
}
