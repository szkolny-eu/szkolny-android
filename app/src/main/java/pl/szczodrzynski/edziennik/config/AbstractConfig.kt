/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config

interface AbstractConfig {
    fun set(key: String, value: String?)
}