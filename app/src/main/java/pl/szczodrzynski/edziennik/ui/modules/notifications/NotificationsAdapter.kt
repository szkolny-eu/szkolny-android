package pl.szczodrzynski.edziennik.ui.modules.notifications

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.modules.notification.Notification
import pl.szczodrzynski.edziennik.data.db.modules.notification.getNotificationTitle
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date

class NotificationsAdapter(
        private val context: Context
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {
    companion object {
        private const val TAG = "NotificationsAdapter"
    }

    var items = listOf<Notification>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.row_notifications_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = context.applicationContext as App

        val notification = items[position]

        val date = Date.fromMillis(notification.addedDate).formattedString
        val colorSecondary = android.R.attr.textColorSecondary.resolveAttr(context)

        holder.title.text = notification.text
        holder.profileDate.text = listOf(
                notification.profileName ?: "",
                " • ",
                date.asColoredSpannable(colorSecondary)
        ).concat()
        holder.type.text = context.getNotificationTitle(notification.type)

        holder.root.onClick {
            val intent = Intent("android.intent.action.MAIN")
            notification.fillIntent(intent)

            d(TAG, "notification with item " + notification.viewId + " extras " + if (intent.extras == null) "null" else intent.extras!!.toString())

            //Log.d(TAG, "Got date "+intent.getLongExtra("timetableDate", 0));

            if (notification.profileId != -1 && notification.profileId != app.profile.id && context is Activity) {
                Toast.makeText(app, app.getString(R.string.toast_changing_profile), Toast.LENGTH_LONG).show()
            }
            app.sendBroadcast(intent)
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var root = itemView
        var title: TextView = itemView.findViewById(R.id.title)
        var profileDate: TextView = itemView.findViewById(R.id.profileDate)
        var type: TextView = itemView.findViewById(R.id.type)
    }
}
