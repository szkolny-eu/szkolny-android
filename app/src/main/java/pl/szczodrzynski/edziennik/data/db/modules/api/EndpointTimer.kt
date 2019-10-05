/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-3.
 */

package pl.szczodrzynski.edziennik.data.db.modules.api

import androidx.room.ColumnInfo
import androidx.room.Entity

const val SYNC_NEVER = 0L
const val SYNC_ALWAYS = 1L

@Entity(tableName = "endpointTimers",
        primaryKeys = ["profileId", "endpointId"])
data class EndpointTimer (

        val profileId: Int,

        @ColumnInfo(name = "endpointId")
        val endpointId: Int,

        @ColumnInfo(name = "endpointLastSync")
        var lastSync: Long? = null,

        @ColumnInfo(name = "endpointNextSync")
        var nextSync: Long = SYNC_ALWAYS,

        @ColumnInfo(name = "endpointViewId")
        var viewId: Int? = null

) {

    /**
     * Tell this timer that an endpoint has just been synced.
     */
    fun syncedNow(): EndpointTimer {
        lastSync = System.currentTimeMillis()
        return this
    }

    /**
     * This will "schedule" the next sync.
     *
     * @param nextSyncIn value in seconds
     */
    fun syncIn(nextSyncIn: Long): EndpointTimer {
        nextSync = System.currentTimeMillis() + nextSyncIn*1000
        return this
    }

    /**
     * Set this timer to sync only if [viewId] is the only
     * selected feature during the current process.
     *
     * [viewId] may be [DRAWER_ITEM_HOME] to sync only if all features are selected.
     */
    fun syncWhenView(viewId: Int): EndpointTimer {
        // set to never sync if nextSync is not already a timestamp
        if (nextSync < 10) {
            this.nextSync = SYNC_NEVER
        }
        this.viewId = viewId
        return this
    }

    /**
     * Set this endpoint to always sync.
     */
    fun syncAlways(): EndpointTimer {
        nextSync = SYNC_ALWAYS
        viewId = null
        return this
    }

    /**
     * This is a suicide as this endpoint will never be synced again.
     */
    fun syncNever(): EndpointTimer {
        nextSync = SYNC_NEVER
        viewId = null
        return this
    }
}