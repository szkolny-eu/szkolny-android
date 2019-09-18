package pl.szczodrzynski.edziennik.models.db

import pl.szczodrzynski.navlib.drawer.IUnreadCounter

class UnreadCounter : IUnreadCounter {
    override var profileId: Int = 0
    override var count: Int = 0
    var thingType: Int = 0

    override var drawerItemId: Int? = null
    override var type: Int
        get() = thingType
        set(value) { thingType = value }
}
