/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.api.v2.events.task

import android.content.Context
import android.content.Intent
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.ApiService
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

abstract class IApiTask(open val profileId: Int) {
    var taskId: Int = 0
    var profile: Profile? = null
    var taskName: String? = null

    /**
     * A method called before running the task.
     * It is synchronous and its main task is
     * to prepare the correct task name.
     */
    abstract fun prepare(app: App)
    abstract fun cancel()

    fun enqueue(context: Context) {
        context.startService(Intent(context, ApiService::class.java))
        EventBus.getDefault().postSticky(this)
    }

    override fun toString(): String {
        return "IApiTask(profileId=$profileId, taskId=$taskId, profile=$profile, taskName=$taskName)"
    }
}