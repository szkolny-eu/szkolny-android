/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.api.v2

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.events.requests.MessageGetRequest
import pl.szczodrzynski.edziennik.api.v2.events.SyncProgressEvent
import pl.szczodrzynski.edziennik.api.v2.events.requests.SyncProfileRequest
import pl.szczodrzynski.edziennik.api.v2.events.requests.SyncRequest
import pl.szczodrzynski.edziennik.api.v2.events.requests.SyncViewRequest
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.api.v2.librus.Librus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.api.v2.models.ApiTask
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile
import kotlin.math.min

class ApiService : Service() {
    companion object {
        const val TAG = "ApiService"
        const val NOTIFICATION_API_CHANNEL_ID = "pl.szczodrzynski.edziennik.GET_DATA"
    }

    private val app by lazy { applicationContext as App }

    private val taskQueue = mutableListOf<ApiTask>()
    private val errorList = mutableListOf<ApiError>()

    private var taskRunning = false
    private var taskRunningId = -1
    private var taskMaximumId = 0

    private var taskProfileId = -1
    private var taskProfileName: String? = null
    private var taskProgress = 0
    private var taskProgressRes: Int? = null

    private val taskCallback = object : EdziennikCallback {
        override fun onCompleted() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onError(apiError: ApiError) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onProgress(step: Int) {
            taskProgress += step
            taskProgress = min(100, taskProgress)
            EventBus.getDefault().post(SyncProgressEvent(taskProfileId, taskProfileName, taskProgress, taskProgressRes))
        }

        override fun onStartProgress(stringRes: Int) {

        }
    }

    private fun sync() {
        if (taskRunning)
            return
        if (taskQueue.size <= 0)
            return // TODO stopSelf() or sth

        val task = taskQueue.removeAt(0)
        taskRunning = true
        taskRunningId = task.taskId

        // get the requested profile and login store
        val profile: Profile? = app.db.profileDao().getByIdNow(task.profileId)
        if (profile == null || !profile.syncEnabled) {
            return
        }
        val loginStore: LoginStore? = app.db.loginStoreDao().getByIdNow(profile.loginStoreId)
        if (loginStore == null) {
            return
        }
        // save the profile ID and name as the current task's
        taskProfileId = profile.id
        taskProfileName = profile.name
        taskProgress = 0
        taskProgressRes = null


        val edziennikInterface = when (loginStore.type) {
            LOGIN_TYPE_LIBRUS -> Librus(app, profile, loginStore, taskCallback)
            else -> null
        }
        if (edziennikInterface == null) {
            return
        }

        when (task) {
            is SyncProfileRequest -> edziennikInterface.sync(task.featureIds ?: Features.getAllIds())
            is SyncViewRequest -> edziennikInterface.sync(Features.getIdsByView(task.targetId))
            is MessageGetRequest -> edziennikInterface.getMessage(task.messageId)
        }
    }


    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onSyncRequest(syncRequest: SyncRequest) {
        app.db.profileDao().idsForSyncNow.forEach { id ->
            taskQueue += SyncProfileRequest(id, null)
        }
        sync()
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onSyncProfileRequest(syncProfileRequest: SyncProfileRequest) {
        Log.d(TAG, syncProfileRequest.toString())
        taskQueue += syncProfileRequest
        sync()
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onMessageGetRequest(messageGetRequest: MessageGetRequest) {
        Log.d(TAG, messageGetRequest.toString())
        taskQueue += messageGetRequest
        sync()
    }

    private val notification by lazy {
        NotificationCompat.Builder(this, NOTIFICATION_API_CHANNEL_ID)
                .setContentTitle("API")
                .setContentText("API is running")
                .setSmallIcon(R.drawable.ic_notification)
                .build()
    }

    override fun onCreate() {
        EventBus.getDefault().register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, notification)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}