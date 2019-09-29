/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.api.v2.interfaces

interface EdziennikInterface {
    fun sync(featureIds: List<Int>)
    fun getMessage(messageId: Int)
}