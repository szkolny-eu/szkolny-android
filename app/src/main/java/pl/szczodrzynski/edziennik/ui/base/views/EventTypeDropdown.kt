/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-14.
 */

package pl.szczodrzynski.edziennik.ui.base.views

import android.content.Context
import android.util.AttributeSet
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.EventType
import pl.szczodrzynski.edziennik.ext.toDrawable
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
            val types = db.eventTypeDao().getAllNow(profileId)
                .sortedBy { it.order }

            list += types.map {
                Item(
                    id = it.id,
                    text = it.name,
                    tag = it,
                    icon = CommunityMaterial.Icon.cmd_circle.toDrawable(it.color),
                )
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
