package pl.szczodrzynski.navlib.bottomsheet.items

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.navlib.ImageHolder
import pl.szczodrzynski.navlib.R
import pl.szczodrzynski.navlib.colorAttr
import pl.szczodrzynski.navlib.getColorFromAttr

data class BottomSheetPrimaryItem(override val isContextual: Boolean = true) : IBottomSheetItem<BottomSheetPrimaryItem.ViewHolder> {

    /*_                             _
     | |                           | |
     | |     __ _ _   _  ___  _   _| |_
     | |    / _` | | | |/ _ \| | | | __|
     | |___| (_| | |_| | (_) | |_| | |_
     |______\__,_|\__, |\___/ \__,_|\__|
                   __/ |
                  |__*/
    override var id: Int = -1
    override val viewType: Int
        get() = 1
    override val layoutId: Int
        get() = R.layout.nav_bs_item_primary

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root = itemView.findViewById<View>(R.id.item_root)
        val image = itemView.findViewById<ImageView>(R.id.item_icon)
        val text = itemView.findViewById<TextView>(R.id.item_text)
        val description = itemView.findViewById<TextView>(R.id.item_description)
    }

    override fun bindViewHolder(viewHolder: ViewHolder) {
        viewHolder.root.setOnClickListener(onClickListener)

        viewHolder.image.setImageDrawable(IconicsDrawable(viewHolder.text.context).apply {
            icon = iconicsIcon
            colorAttr(viewHolder.text.context, android.R.attr.textColorSecondary)
            sizeDp = 24
        })

        viewHolder.description.visibility = View.VISIBLE
        when {
            descriptionRes != null -> viewHolder.description.setText(descriptionRes!!)
            description != null -> viewHolder.description.text = description
            else -> viewHolder.description.visibility = View.GONE
        }

        when {
            titleRes != null -> viewHolder.text.setText(titleRes!!)
            else -> viewHolder.text.text = title
        }
        viewHolder.text.setTextColor(getColorFromAttr(viewHolder.text.context, android.R.attr.textColorPrimary))
    }

    /*_____        _
     |  __ \      | |
     | |  | | __ _| |_ __ _
     | |  | |/ _` | __/ _` |
     | |__| | (_| | || (_| |
     |_____/ \__,_|\__\__,*/
    var title: CharSequence? = null
    @StringRes
    var titleRes: Int? = null
    var description: CharSequence? = null
    @StringRes
    var descriptionRes: Int? = null
    var icon: ImageHolder? = null
    var iconicsIcon: IIcon? = null
    var onClickListener: View.OnClickListener? = null

    fun withId(id: Int): BottomSheetPrimaryItem {
        this.id = id
        return this
    }

    fun withTitle(title: CharSequence): BottomSheetPrimaryItem {
        this.title = title
        this.titleRes = null
        return this
    }
    fun withTitle(@StringRes title: Int): BottomSheetPrimaryItem {
        this.title = null
        this.titleRes = title
        return this
    }

    fun withDescription(description: CharSequence): BottomSheetPrimaryItem {
        this.description = description
        this.descriptionRes = null
        return this
    }
    fun withDescription(@StringRes description: Int): BottomSheetPrimaryItem {
        this.description = null
        this.descriptionRes = description
        return this
    }

    fun withIcon(icon: Drawable): BottomSheetPrimaryItem {
        this.icon = ImageHolder(icon)
        return this
    }
    fun withIcon(@DrawableRes icon: Int): BottomSheetPrimaryItem {
        this.icon = ImageHolder(icon)
        return this
    }
    fun withIcon(icon: IIcon): BottomSheetPrimaryItem {
        this.iconicsIcon = icon
        return this
    }

    fun withOnClickListener(onClickListener: View.OnClickListener): BottomSheetPrimaryItem {
        this.onClickListener = onClickListener
        return this
    }
}
