/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-3.
 */

package pl.szczodrzynski.edziennik.data.db.modules.api

import androidx.room.ColumnInfo
import androidx.room.Entity

const val SYNC_NEVER = 0L
const val SYNC_ALWAYS = 1L
const val SYNC_IF_EXPLICIT = 2L
const val SYNC_IF_EXPLICIT_OR_ALL = 3L

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
        viewId = null
        return this
    }

    /**
     * Set this timer to sync only if [viewId] is the only
     * selected feature during the current process.
     */
    fun syncWhenView(viewId: Int, syncIfAll: Boolean = false): EndpointTimer {
        nextSync = if (syncIfAll) SYNC_IF_EXPLICIT_OR_ALL else SYNC_IF_EXPLICIT
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