/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-17.
 */

package pl.szczodrzynski.edziennik.ui.modules.settings

import android.content.Context
import android.view.View
import com.danielstone.materialaboutlibrary.holders.MaterialAboutItemViewHolder
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem

class MaterialAboutProfileItem(item: MaterialAboutTitleItem) : MaterialAboutTitleItem(item) {
    companion object {
        fun getViewHolder(view: View): MaterialAboutItemViewHolder =
            MaterialAboutTitleItem.getViewHolder(view)

        fun setupItem(
            holder: MaterialAboutTitleItemViewHolder,
            item: MaterialAboutProfileItem,
            context: Context
        ) = MaterialAboutTitleItem.setupItem(holder, item, context)
    }

    override fun getType(): Int {
        return SettingsViewTypeManager.ItemType.PROFILE_ITEM
    }

    override fun getDetailString() = "MaterialAboutProfileItem{" +
            "text=" + text +
            ", textRes=" + textRes +
            ", desc=" + desc +
            ", descRes=" + descRes +
            ", icon=" + icon +
            ", iconRes=" + iconRes +
            ", onClickAction=" + onClickAction +
            ", onLongClickAction=" + onLongClickAction +
            '}'

    override fun clone() = MaterialAboutProfileItem(this)
}
