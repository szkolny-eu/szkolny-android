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
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.databinding.TeacherItemBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.messages.MessagesUtils.getProfileImage
import pl.szczodrzynski.navlib.colorAttr
import java.util.*
import kotlin.concurrent.schedule
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
        b.image.setImageBitmap(item.image)
        b.type.text = item.getTypeName(activity, App.db.subjectDao().getAllNow(App.profileId))
        b.copy.isVisible = true
        b.copy.setImageDrawable(IconicsDrawable(activity, CommunityMaterial.Icon.cmd_clipboard_text_multiple_outline).apply {
            colorAttr(activity, R.attr.colorIcon)
            sizeDp = 24
        })
        b.copy.onClick {
            item.fullName.copyToClipboard(activity)
            Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
        if (item.loginId.isNotNullNorBlank()) {
            b.sendMessage.isVisible = true
            b.sendMessage.setImageDrawable(IconicsDrawable(activity,
                CommunityMaterial.Icon.cmd_email_plus_outline).apply {
                colorAttr(activity,
                    R.attr.colorIcon); sizeDp = 24
            })

            b.sendMessage.onClick {
                val intent = Intent(
                    Intent.ACTION_MAIN,
                    "fragmentId" to MainActivity.TARGET_MESSAGES_COMPOSE,
                    "messageRecipientId" to item.id
                )
                activity.sendBroadcast(intent)
            }
        }

    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: TeacherItemBinding) : RecyclerView.ViewHolder(b.root)
}
