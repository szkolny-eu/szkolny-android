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
import pl.szczodrzynski.edziennik.api.v2.events.*
import pl.szczodrzynski.edziennik.api.v2.events.requests.*
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikInterface
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
    private var queueHasErrorReportTask = false

    private var taskCancelled = false
    private var taskRunning = false
    private var taskRunningId = -1
    private var taskMaximumId = 0
    private var edziennikInterface: EdziennikInterface? = null

    private var taskProfileId = -1
    private var taskProfileName: String? = null
    private var taskProgress = 0
    private var taskProgressRes: Int? = null

    private val notification by lazy { EdziennikNotification(this) }

    /*    ______    _     _                  _ _       _____      _ _ _                _
         |  ____|  | |   (_)                (_) |     / ____|    | | | |              | |
         | |__   __| |_____  ___ _ __  _ __  _| | __ | |     __ _| | | |__   __ _  ___| | __
         |  __| / _` |_  / |/ _ \ '_ \| '_ \| | |/ / | |    / _` | | | '_ \ / _` |/ __| |/ /
         | |___| (_| |/ /| |  __/ | | | | | | |   <  | |___| (_| | | | |_) | (_| | (__|   <
         |______\__,_/___|_|\___|_| |_|_| |_|_|_|\_\  \_____\__,_|_|_|_.__/ \__,_|\___|_|\*/
    private val taskCallback = object : EdziennikCallback {
        override fun onCompleted() {
            edziennikInterface = null
            if (!taskCancelled) {
                EventBus.getDefault().post(SyncProfileFinishedEvent(taskProfileId))
            }
            notification.setIdle().post()
            taskRunning = false
            taskRunningId = -1
            sync()
        }

        override fun onError(apiError: ApiError) {
            if (!queueHasErrorReportTask) {
                queueHasErrorReportTask = true
                taskQueue += ErrorReportTask().apply {
                    taskId = ++taskMaximumId
                }
            }
            EventBus.getDefault().post(SyncErrorEvent(apiError))
            errorList.add(apiError)
            if (apiError.isCritical) {
                notification.setCriticalError().post()
                taskRunning = false
                taskRunningId = -1
                sync()
            }
            else {
                notification.addError().post()
            }
        }

        override fun onProgress(step: Int) {
            taskProgress += step
            taskProgress = min(100, taskProgress)
            EventBus.getDefault().post(SyncProgressEvent(taskProfileId, taskProfileName, taskProgress, taskProgressRes))
            notification.setProgress(taskProgress).post()
        }

        override fun onStartProgress(stringRes: Int) {
            taskProgressRes = stringRes
            EventBus.getDefault().post(SyncProgressEvent(taskProfileId, taskProfileName, taskProgress, taskProgressRes))
            notification.setProgressRes(taskProgressRes!!).post()
        }
    }

    /*    _______        _                               _   _
         |__   __|      | |                             | | (_)
            | | __ _ ___| | __   _____  _____  ___ _   _| |_ _  ___  _ __
            | |/ _` / __| |/ /  / _ \ \/ / _ \/ __| | | | __| |/ _ \| '_ \
            | | (_| \__ \   <  |  __/>  <  __/ (__| |_| | |_| | (_) | | | |
            |_|\__,_|___/_|\_\  \___/_/\_\___|\___|\__,_|\__|_|\___/|_| |*/
    private fun sync() {
        if (taskRunning)
            return
        if (taskQueue.size <= 0) {
            allCompleted()
            return
        }

        val task = taskQueue.removeAt(0)
        taskCancelled = false
        taskRunning = true
        taskRunningId = task.taskId

        if (task is ErrorReportTask) {
            notification
                    .setCurrentTask(taskRunningId, null)
                    .setProgressRes(R.string.edziennik_notification_api_error_report_title)
            return
        }

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

        // update the notification
        notification.setCurrentTask(taskRunningId, taskProfileName).post()

        edziennikInterface = when (loginStore.type) {
            LOGIN_TYPE_LIBRUS -> Librus(app, profile, loginStore, taskCallback)
            else -> null
        }
        if (edziennikInterface == null) {
            return
        }

        when (task) {
            is SyncProfileRequest -> edziennikInterface?.sync(task.featureIds ?: Features.getAllIds())
            is SyncViewRequest -> edziennikInterface?.sync(Features.getIdsByView(task.targetId))
            is MessageGetRequest -> edziennikInterface?.getMessage(task.messageId)
        }
    }

    private fun allCompleted() {
        EventBus.getDefault().post(SyncFinishedEvent())
        stopSelf()
    }

    /*    ______               _   ____
         |  ____|             | | |  _ \
         | |____   _____ _ __ | |_| |_) |_   _ ___
         |  __\ \ / / _ \ '_ \| __|  _ <| | | / __|
         | |___\ V /  __/ | | | |_| |_) | |_| \__ \
         |______\_/ \___|_| |_|\__|____/ \__,_|__*/
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onSyncRequest(syncRequest: SyncRequest) {
        app.db.profileDao().idsForSyncNow.forEach { id ->
            taskQueue += SyncProfileRequest(id, null).apply {
                taskId = ++taskMaximumId
            }
        }
        sync()
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onSyncProfileRequest(syncProfileRequest: SyncProfileRequest) {
        Log.d(TAG, syncProfileRequest.toString())
        taskQueue += syncProfileRequest.apply {
            taskId = ++taskMaximumId
        }
        sync()
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onSyncViewRequest(syncViewRequest: SyncViewRequest) {
        Log.d(TAG, syncViewRequest.toString())
        taskQueue += syncViewRequest.apply {
            taskId = ++taskMaximumId
        }
        sync()
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onMessageGetRequest(messageGetRequest: MessageGetRequest) {
        Log.d(TAG, messageGetRequest.toString())
        taskQueue += messageGetRequest.apply {
            taskId = ++taskMaximumId
        }
        sync()
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onTaskCancelRequest(taskCancelRequest: TaskCancelRequest) {
        taskCancelled = true
        edziennikInterface?.cancel()
    }

    /*     _____                 _                                     _     _
          / ____|               (_)                                   (_)   | |
         | (___   ___ _ ____   ___  ___ ___    _____   _____ _ __ _ __ _  __| | ___  ___
          \___ \ / _ \ '__\ \ / / |/ __/ _ \  / _ \ \ / / _ \ '__| '__| |/ _` |/ _ \/ __|
          ____) |  __/ |   \ V /| | (_|  __/ | (_) \ V /  __/ |  | |  | | (_| |  __/\__ \
         |_____/ \___|_|    \_/ |_|\___\___|  \___/ \_/ \___|_|  |_|  |_|\__,_|\___||__*/
    override fun onCreate() {
        EventBus.getDefault().register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(EdziennikNotification.NOTIFICATION_ID, notification.notification)
        notification.setIdle().setCloseAction().post()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}