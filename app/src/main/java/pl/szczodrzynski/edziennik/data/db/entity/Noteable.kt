/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.entity

interface Noteable {

    fun getNoteType(): Note.OwnerType
}
