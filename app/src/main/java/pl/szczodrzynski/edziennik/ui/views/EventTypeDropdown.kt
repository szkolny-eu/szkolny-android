/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-14.
 */

package pl.szczodrzynski.edziennik.ui.views

import android.content.Context
import android.util.AttributeSet
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.EventType
import pl.szczodrzynski.edziennik.utils.TextInputDropDown

class EventTypeDropdown : TextInputDropDown {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    lateinit var db: AppDb
    var profileId: Int = 0
    var onTypeSelected: ((eventType: EventType) -> Unit)? = null

    override fun create(context: Context) {
        super.create(context)
        isEnabled = false
    }

    suspend fun loadItems() {
        val types = withContext(Dispatchers.Default) {
            val list = mutableListOf<Item>()

            var types = db.eventTypeDao().getAllNow(profileId)

            if (types.none { it.id in -1L..10L }) {
                types = db.eventTypeDao().addDefaultTypes(context, profileId)
            }

            list += types.map {
                Item(it.id, it.name, tag = it, icon = IconicsDrawable(context).apply {
                    icon = CommunityMaterial.Icon.cmd_circle
                    sizeDp = 24
                    colorInt = it.color
                })
            }

            list
        }

        clear().append(types)
        isEnabled = true

        setOnChangeListener {
            when (it.tag) {
                is EventType -> {
                    // selected an event type
                    onTypeSelected?.invoke(it.tag)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Select an event type by the [typeId].
     */
    fun selectType(typeId: Long) = select(typeId)

    /**
     * Select an event type by the [typeId] **if it's not selected yet**.
     */
    fun selectDefault(typeId: Long?) {
        if (typeId == null || selected != null)
            return
        selectType(typeId)
    }

    /**
     * Get the currently selected event type.
     * ### Returns:
     * - null if no valid type is selected
     * - [EventType] - the selected event type
     */
    fun getSelected(): EventType? {
        return when (selected?.tag) {
            is EventType -> selected?.tag as EventType
            else -> null
        }
    }
}
