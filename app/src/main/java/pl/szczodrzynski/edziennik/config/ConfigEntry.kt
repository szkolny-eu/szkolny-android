/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import androidx.room.Entity

@Entity(tableName = "config", primaryKeys = ["profileId", "key"])
data class ConfigEntry(
        val profileId: Int = -1,
        val key: String,
        val value: String?
)