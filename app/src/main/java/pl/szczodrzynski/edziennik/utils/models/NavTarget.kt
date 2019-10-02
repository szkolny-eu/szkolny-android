package pl.szczodrzynski.edziennik.utils.models

import androidx.fragment.app.Fragment
import com.mikepenz.iconics.typeface.IIcon
import kotlin.reflect.KClass

data class NavTarget(
        val id: Int,
        val name: Int,
        val fragmentClass: KClass<out Fragment>?
) {
    var title: Int? = null
    var icon: IIcon? = null
    var description: Int? = null
    var isInDrawer: Boolean = false
    var isInProfileList: Boolean = false
    var isStatic: Boolean = false
    var isBelowSeparator: Boolean = false
    var popToHome: Boolean = false
    var badgeTypeId: Int? = null
    var canHideInDrawer: Boolean = true
    var canHideInMiniDrawer: Boolean = true
    var selectable: Boolean = true
    var subItems: Array<out NavTarget>? = null

    fun withTitle(title: Int?): NavTarget {
        this.title = title
        return this
    }

    fun withIcon(icon: IIcon?): NavTarget{
        this.icon = icon
        return this
    }

    fun withDescription(description: Int?): NavTarget {
        this.description = description
        return this
    }

    fun isInDrawer(isInDrawer: Boolean): NavTarget {
        this.isInDrawer = isInDrawer
        this.popToHome = true
        return this
    }

    fun isInProfileList(isInProfileList: Boolean): NavTarget {
        this.isInProfileList = isInProfileList
        return this
    }

    fun isStatic(isStatic: Boolean): NavTarget {
        this.isStatic = isStatic
        return this
    }

    fun isBelowSeparator(isBelowSeparator: Boolean): NavTarget {
        this.isBelowSeparator = isBelowSeparator
        return this
    }

    fun withPopToHome(popToHome: Boolean): NavTarget {
        this.popToHome = popToHome
        return this
    }

    fun withBadgeTypeId(badgeTypeId: Int?): NavTarget {
        this.badgeTypeId = badgeTypeId
        return this
    }

    fun canHideInDrawer(canHideInDrawer: Boolean): NavTarget {
        this.canHideInDrawer = canHideInDrawer
        return this
    }

    fun canHideInMiniDrawer(canHideInMiniDrawer: Boolean): NavTarget {
        this.canHideInMiniDrawer = canHideInMiniDrawer
        return this
    }

    fun withSelectable(selectable: Boolean): NavTarget {
        this.selectable = selectable
        return this
    }

    fun withSubItems(vararg items: NavTarget): NavTarget {
        this.subItems = items
        return this
    }
}
