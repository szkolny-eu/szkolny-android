/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-27.
 */

package pl.szczodrzynski.edziennik.data.config

import pl.szczodrzynski.edziennik.data.db.AppDb

abstract class BaseMigration<T> {

    lateinit var db: AppDb
    var profileId: Int = -1
    val profile by lazy { db.profileDao().getByIdNow(profileId) }

    abstract fun migrate(config: T): Any
}
