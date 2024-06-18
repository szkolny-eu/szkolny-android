package pl.szczodrzynski.navlib.drawer.items

import com.mikepenz.materialdrawer.model.PrimaryDrawerItem

class DrawerPrimaryItem : PrimaryDrawerItem() {
    var appTitle: String? = null
    fun withAppTitle(appTitle: String?): PrimaryDrawerItem {
        this.appTitle = appTitle
        return this
    }
}

fun PrimaryDrawerItem.withAppTitle(appTitle: String?): PrimaryDrawerItem {
    if (this !is DrawerPrimaryItem)
        return this
    this.appTitle = appTitle
    return this
}