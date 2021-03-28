/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-17.
 */

package pl.szczodrzynski.edziennik.ui.modules.settings

import android.content.Context
import android.view.View
import com.danielstone.materialaboutlibrary.holders.MaterialAboutItemViewHolder
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem.MaterialAboutTitleItemViewHolder
import com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.modules.settings.SettingsViewTypeManager.ItemType.Companion.PROFILE_ITEM

class SettingsViewTypeManager : DefaultViewTypeManager() {
    class ItemType {
        companion object {
            const val PROFILE_ITEM = 10
        }
    }

    override fun getLayout(itemType: Int) = when (itemType) {
        PROFILE_ITEM -> R.layout.mal_material_about_profile_item
        else -> super.getLayout(itemType)
    }

    override fun getViewHolder(itemType: Int, view: View): MaterialAboutItemViewHolder =
        when (itemType) {
            PROFILE_ITEM -> MaterialAboutProfileItem.getViewHolder(view)
            else -> super.getViewHolder(itemType, view)
        }

    override fun setupItem(
        itemType: Int,
        holder: MaterialAboutItemViewHolder,
        item: MaterialAboutItem,
        context: Context
    ) = when (itemType) {
        PROFILE_ITEM -> MaterialAboutProfileItem.setupItem(
            holder as MaterialAboutTitleItemViewHolder,
            item as MaterialAboutProfileItem, context
        )
        else -> super.setupItem(itemType, holder, item, context)
    }
}
