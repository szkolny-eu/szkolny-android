/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-7.
 */

package pl.szczodrzynski.edziennik.data.api.task

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.HOUR
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.utils.Utils.d

class SzkolnyTask(val app: App, val syncingProfiles: List<Profile>) : IApiTask(-1) {
    companion object {
        private const val TAG = "SzkolnyTask"
    }
    private val api by lazy { SzkolnyApi(app) }
    private val profiles by lazy { app.db.profileDao().allNow }
    override fun prepare(app: App) { taskName = app.getString(R.string.edziennik_szkolny_creating_notifications) }
    override fun cancel() {}

    private val notificationList = mutableListOf<Notification>()

    internal fun run(taskCallback: EdziennikCallback) {
        val startTime = System.currentTimeMillis()

        // create all e-register data notifications
        val notifications = Notifications(app, notificationList, profiles)
        notifications.run()

        val shouldAppSync = notificationList.isNotEmpty() || (System.currentTimeMillis() - app.config.lastAppSync > 24*HOUR*1000)
        // do an AppSync every 24 hours, or if WebPush has a notification
        if (shouldAppSync) {
            // send notifications to web push, get shared events
            val addedEvents = AppSync(app, notificationList, profiles, api).run()
            if (addedEvents > 0) {
                // create notifications for shared events (not present before app sync)
                notifications.sharedEventNotifications()
            }
            app.config.lastAppSync = System.currentTimeMillis()
        }
        d(TAG, "Created ${notificationList.count()} notifications.")

        // update the database
        app.db.metadataDao().setAllNotified(true)
        if (notificationList.isNotEmpty())
            app.db.notificationDao().addAll(notificationList)
        app.db.profileDao().setAllNotEmpty()

        // post all notifications
        PostNotifications(app, notificationList)
        d(TAG, "SzkolnyTask: finished in ${System.currentTimeMillis()-startTime} ms.")
        taskCallback.onCompleted()
    }
}
