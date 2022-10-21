/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.config.db.ConfigEntry
import pl.szczodrzynski.edziennik.config.utils.config
import pl.szczodrzynski.edziennik.data.api.szkolny.response.RegisterAvailabilityStatus
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.home.HomeCardModel
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.coroutines.CoroutineContext

class BaseConfig(
    val app: App,
    val profileId: Int? = null,
) : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    val values = hashMapOf<String, String?>()

    init {
        values.clear()
        for ((_, key, value) in app.db.configDao().getAllNow(profileId ?: -1)) {
            values[key] = value
        }
    }

    fun set(key: String, value: String?) {
        values[key] = value
        launch(Dispatchers.IO) {
            app.db.configDao().add(ConfigEntry(profileId ?: -1, key, value))
        }
    }
}
