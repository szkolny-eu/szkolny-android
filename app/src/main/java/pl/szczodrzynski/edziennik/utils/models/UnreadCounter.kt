package pl.szczodrzynski.edziennik.utils.models

import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.asMetadataType
import pl.szczodrzynski.navlib.drawer.IUnreadCounter

class UnreadCounter : IUnreadCounter {
    override var profileId: Int = 0
    override var count: Int = 0
    lateinit var thingType: MetadataType

    override var drawerItemId: Int? = null
    override var type: Int
        get() = thingType.id
        set(value) { thingType = value.asMetadataType() }
}
