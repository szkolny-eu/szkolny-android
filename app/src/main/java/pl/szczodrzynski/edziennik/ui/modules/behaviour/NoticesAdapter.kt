package pl.szczodrzynski.edziennik.ui.modules.behaviour

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.mikepenz.iconics.IconicsDrawable
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice
import pl.szczodrzynski.edziennik.data.db.modules.notices.NoticeFull
import pl.szczodrzynski.edziennik.utils.models.Date

import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_MOBIDZIENNIK
import pl.szczodrzynski.edziennik.utils.Utils.bs

class NoticesAdapter//getting the context and product list with constructor
(private val context: Context, var noticeList: List<NoticeFull>) : RecyclerView.Adapter<NoticesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //inflating and returning our view holder
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.row_notices_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = context.applicationContext as App

        val notice = noticeList[position]

        if (app.profile.loginStoreType == LOGIN_TYPE_MOBIDZIENNIK && false) {
            holder.noticesItemReason.text = bs(null, notice.category, "\n") + notice.text
            holder.noticesItemTeacherName.text = app.getString(R.string.notices_points_format, notice.teacherFullName, if (notice.points > 0) "+" + notice.points else notice.points)
        } else {
            holder.noticesItemReason.text = notice.text
            holder.noticesItemTeacherName.text = notice.teacherFullName
        }
        holder.noticesItemAddedDate.text = Date.fromMillis(notice.addedDate).formattedString

        if (notice.type == Notice.TYPE_POSITIVE) {
            holder.noticesItemType.setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon2.cmd_plus_circle)
                    .colorRes(R.color.md_green_600)
                    .sizeDp(36))
        } else if (notice.type == Notice.TYPE_NEGATIVE) {
            holder.noticesItemType.setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon.cmd_alert_decagram)
                    .colorRes(R.color.md_red_600)
                    .sizeDp(36))
        } else {
            holder.noticesItemType.setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon2.cmd_message_outline)
                    .colorRes(R.color.md_blue_500)
                    .sizeDp(36))
        }

        if (!notice.seen) {
            holder.noticesItemReason.background = context.resources.getDrawable(R.drawable.bg_rounded_8dp)
            when {
                notice.type == Notice.TYPE_POSITIVE -> holder.noticesItemReason.background.colorFilter = PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY)
                notice.type == Notice.TYPE_NEGATIVE -> holder.noticesItemReason.background.colorFilter = PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY)
                else -> holder.noticesItemReason.background.colorFilter = PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY)
            }
            notice.seen = true
            AsyncTask.execute { app.db.metadataDao().setSeen(App.profileId, notice, true) }
        } else {
            holder.noticesItemReason.background = null
        }
    }

    override fun getItemCount(): Int {
        return noticeList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var noticesItemCard: CardView = itemView.findViewById(R.id.noticesItemCard)
        var noticesItemType: ImageView = itemView.findViewById(R.id.noticesItemType)
        var noticesItemReason: TextView = itemView.findViewById(R.id.noticesItemReason)
        var noticesItemTeacherName: TextView = itemView.findViewById(R.id.noticesItemTeacherName)
        var noticesItemAddedDate: TextView = itemView.findViewById(R.id.noticesItemAddedDate)
    }
}
