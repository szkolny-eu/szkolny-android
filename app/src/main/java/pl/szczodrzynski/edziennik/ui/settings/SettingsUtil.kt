/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-17.
 */

package pl.szczodrzynski.edziennik.ui.settings

import com.danielstone.materialaboutlibrary.items.*
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ext.after
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.Utils

class SettingsUtil(
    val activity: MainActivity,
    private val onRefresh: () -> Unit
) {

    fun refresh() = onRefresh()

    private fun IIcon.asDrawable(color: Int? = null, size: Int = 24) =
        IconicsDrawable(activity).apply {
            icon = this@asDrawable
            sizeDp = size
            colorInt = color ?: Themes.getPrimaryTextColor(activity)
        }

    fun createCard(
        titleRes: Int?,
        items: (card: MaterialAboutCard) -> List<MaterialAboutItem>,
        itemsMore: (card: MaterialAboutCard) -> List<MaterialAboutItem>,
        backgroundColor: Int? = null,
        theme: Int? = null
    ): MaterialAboutCard {
        val card = MaterialAboutCard.Builder()
            .title(titleRes ?: 0)
            .cardColor(backgroundColor ?: 0)
            .theme(theme ?: 0)
            .outline(false)
            .build()
        card.items.addAll(items(card))

        val more = itemsMore(card)
        if (more.isNotEmpty()) {
            card.items.add(createMoreItem(card, more))
        }

        return card
    }

    fun createMoreItem(
        card: MaterialAboutCard,
        items: List<MaterialAboutItem>
    ): MaterialAboutActionItem {
        val iconColor = card.cardColor.let {
            if (it == 0)
                null
            else
                Colors.legibleTextColor(it)
        }

        val moreItem = MaterialAboutActionItem.Builder()
            .text(R.string.settings_more_text)
            .icon(CommunityMaterial.Icon.cmd_chevron_down.asDrawable(iconColor, size = 24))
            .build()

        moreItem.setOnClickAction {
            card.items.after(moreItem, items)
            card.items.remove(moreItem)
            onRefresh()
        }

        return moreItem
    }

    fun createSectionItem(text: Int) = MaterialAboutSectionItem(text)

    fun createActionItem(
        text: Int,
        subText: Int? = null,
        icon: IIcon,
        backgroundColor: Int? = null,
        onClick: (item: MaterialAboutActionItem) -> Unit
    ): MaterialAboutActionItem {
        val iconColor = backgroundColor?.let { Colors.legibleTextColor(it) }

        val item = MaterialAboutActionItem.Builder()
            .text(text)
            .subText(subText ?: 0)
            .icon(icon.asDrawable(iconColor))
            .build()

        item.setOnClickAction {
            onClick(item)
        }

        return item
    }

    fun createPropertyItem(
        text: Int,
        subText: Int? = null,
        subTextChecked: Int? = null,
        icon: IIcon,
        backgroundColor: Int? = null,
        value: Boolean,
        beforeChange: ((item: MaterialAboutSwitchItem, value: Boolean) -> Boolean)? = null,
        onChange: (item: MaterialAboutSwitchItem, value: Boolean) -> Unit
    ): MaterialAboutSwitchItem {
        val iconColor = backgroundColor?.let { Colors.legibleTextColor(it) }

        val item = MaterialAboutSwitchItem.Builder()
            .text(text)
            .subText(subText ?: 0)
            .subTextChecked(subTextChecked ?: 0)
            .icon(icon.asDrawable(iconColor))
            .setChecked(value)
            .build()

        item.setOnCheckedChangedAction { item, isChecked ->
            if (beforeChange?.invoke(item as MaterialAboutSwitchItem, isChecked) == false)
                return@setOnCheckedChangedAction false
            onChange(item as MaterialAboutSwitchItem, isChecked)
            true
        }

        return item
    }

    fun createPropertyActionItem(
        text: Int,
        subText: Int? = null,
        subTextChecked: Int? = null,
        icon: IIcon,
        backgroundColor: Int? = null,
        value: Boolean,
        onChange: (item: MaterialAboutActionSwitchItem, value: Boolean) -> Unit,
        onClick: (item: MaterialAboutActionSwitchItem) -> Unit
    ): MaterialAboutSwitchItem {
        val iconColor = backgroundColor?.let { Colors.legibleTextColor(it) }

        val item = MaterialAboutActionSwitchItem.Builder()
            .text(text)
            .subText(subText ?: 0)
            .subTextChecked(subTextChecked ?: 0)
            .icon(icon.asDrawable(iconColor))
            .setChecked(value)
            .build()

        item.setOnClickAction {
            onClick(item)
        }
        item.setOnCheckedChangedAction { item, isChecked ->
            onChange(item as MaterialAboutActionSwitchItem, isChecked)
            true
        }

        return item
    }

    fun createTitleItem(): MaterialAboutTitleItem =
        MaterialAboutTitleItem.Builder()
            .text(R.string.app_name)
            .desc(R.string.settings_about_title_subtext)
            .icon(R.mipmap.ic_splash)
            .build()

    fun createProfileItem(
        profile: Profile,
        onClick: (item: MaterialAboutProfileItem, profile: Profile) -> Unit
    ): MaterialAboutProfileItem {
        val item = MaterialAboutProfileItem(
            MaterialAboutTitleItem.Builder()
                .text(profile.name)
                .desc(profile.subname)
                .icon(profile.getImageDrawable(activity))
                .build()
        )
        item.setOnClickAction {
            onClick(item, profile)
        }
        return item
    }
}
