package pl.szczodrzynski.edziennik.ui.behaviour

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.sizeDp
import eu.szkolny.font.SzkolnyFont
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.full.NoticeFull
import pl.szczodrzynski.edziennik.ext.resolveColor
import pl.szczodrzynski.edziennik.utils.BetterLink
import pl.szczodrzynski.edziennik.utils.Utils.bs
import pl.szczodrzynski.edziennik.utils.models.Date

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

        if (app.data.uiConfig.enableNoticePoints && false) {
            holder.noticesItemReason.text = bs(null, notice.category, "\n") + notice.text
            holder.noticesItemTeacherName.text = app.getString(R.string.notices_points_format, notice.teacherName, if (notice.points ?: 0f > 0) "+" + notice.points else notice.points)
        } else {
            holder.noticesItemReason.text = notice.text
            holder.noticesItemTeacherName.text = notice.teacherName
        }
        holder.noticesItemAddedDate.text = Date.fromMillis(notice.addedDate).formattedString

        if (notice.type == Notice.TYPE_POSITIVE) {
            holder.noticesItemType.setImageDrawable(
                IconicsDrawable(context, CommunityMaterial.Icon3.cmd_plus_circle_outline).apply {
                    colorInt = MaterialColors.harmonizeWithPrimary(context, R.color.md_green_600.resolveColor(context))
                    sizeDp = 36
                }
            )
        } else if (notice.type == Notice.TYPE_NEGATIVE) {
            holder.noticesItemType.setImageDrawable(
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_alert_decagram_outline).apply {
                    colorInt = MaterialColors.harmonizeWithPrimary(context, R.color.md_red_600.resolveColor(context))
                    sizeDp = 36
                }
            )
        } else {
            holder.noticesItemType.setImageDrawable(
                IconicsDrawable(context, SzkolnyFont.Icon.szf_message_processing_outline).apply {
                    colorInt = MaterialColors.harmonizeWithPrimary(context, R.color.md_blue_500.resolveColor(context))
                    sizeDp = 36
                }
            )
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

        BetterLink.attach(holder.noticesItemReason)

        notice.teacherName?.let { name ->
            BetterLink.attach(holder.noticesItemTeacherName, teachers = mapOf(
                notice.teacherId to name
            ))
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
