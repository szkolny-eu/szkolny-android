/*
 * Copyright (c) Antoni Czaplicki 2021-10-15.
 */

package pl.szczodrzynski.edziennik.ui.teachers

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Subject
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.databinding.TeacherItemBinding
import pl.szczodrzynski.edziennik.ext.*
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

    var subjectList = listOf<Subject>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(activity)
        val view = TeacherItemBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.b

        b.name.text = item.fullName
        b.image.setImageBitmap(item.image)
        b.type.text = item.getTypeText(activity, subjectList)
        b.copy.isVisible = true
        b.copy.onClick {
            item.fullName.copyToClipboard(activity)
            Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
        b.copy.onLongClick {
            Toast.makeText(activity, R.string.copy_to_clipboard, Toast.LENGTH_SHORT).show()
            true
        }
        if (item.loginId.isNotNullNorBlank()) {
            b.sendMessage.isVisible = true

            b.sendMessage.onClick {
                val intent = Intent(
                    Intent.ACTION_MAIN,
                    "fragmentId" to MainActivity.TARGET_MESSAGES_COMPOSE,
                    "messageRecipientId" to item.id
                )
                activity.sendBroadcast(intent)
            }
            b.sendMessage.onLongClick {
                Toast.makeText(activity, app.getString(R.string.send_message_to, item.fullName), Toast.LENGTH_SHORT).show()
                true
            }
        } else {
            b.sendMessage.isVisible = false
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: TeacherItemBinding) : RecyclerView.ViewHolder(b.root)
}
