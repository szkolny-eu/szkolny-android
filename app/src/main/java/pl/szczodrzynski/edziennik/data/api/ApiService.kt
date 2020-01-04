/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.data.api

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.events.*
import pl.szczodrzynski.edziennik.data.api.events.requests.ServiceCloseRequest
import pl.szczodrzynski.edziennik.data.api.events.requests.TaskCancelRequest
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.task.*
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.utils.Utils.d
import kotlin.math.min
import kotlin.math.roundToInt

class ApiService : Service() {
    companion object {
        const val TAG = "ApiService"
        const val NOTIFICATION_API_CHANNEL_ID = "pl.szczodrzynski.edziennik.GET_DATA"
        fun start(context: Context) {
            context.startService(Intent(context, ApiService::class.java))
        }
        fun startAndRequest(context: Context, request: Any) {
            context.startService(Intent(context, ApiService::class.java))
            EventBus.getDefault().postSticky(request)
        }
    }

    private val app by lazy { applicationContext as App }

    private val syncingProfiles = mutableListOf<Profile>()

    private val finishingTaskQueue = mutableListOf(
            SzkolnyTask.sync(syncingProfiles),
            NotifyTask()
    )
    private val allTaskList = mutableListOf<IApiTask>()
    private val taskQueue = mutableListOf<IApiTask>()
    private val errorList = mutableListOf<ApiError>()

    private var serviceClosed = false
    private var taskCancelled = false
    private var taskIsRunning = false
    private var taskRunning: IApiTask? = null // for debug purposes
    private var taskRunningId = -1
    private var taskMaximumId = 0

    private var taskProfileId = -1
    private var taskProgress = -1f
    private var taskProgressText: String? = null

    private val notification by lazy { EdziennikNotification(this) }

    private var lastEventTime = System.currentTimeMillis()
    private var taskCancelTries = 0

    /*    ______    _     _                  _ _       _____      _ _ _                _
         |  ____|  | |   (_)                (_) |     / ____|    | | | |              | |
         | |__   __| |_____  ___ _ __  _ __  _| | __ | |     __ _| | | |__   __ _  ___| | __
         |  __| / _` |_  / |/ _ \ '_ \| '_ \| | |/ / | |    / _` | | | '_ \ / _` |/ __| |/ /
         | |___| (_| |/ /| |  __/ | | | | | | |   <  | |___| (_| | | | |_) | (_| | (__|   <
         |______\__,_/___|_|\___|_| |_|_| |_|_|_|\_\  \_____\__,_|_|_|_.__/ \__,_|\___|_|\*/
    private val taskCallback = object : EdziennikCallback {
        override fun onCompleted() {
            lastEventTime = System.currentTimeMillis()
            d(TAG, "Task $taskRunningId (profile $taskProfileId) - $taskProgressText - finished")
            EventBus.getDefault().postSticky(ApiTaskFinishedEvent(taskProfileId))
            clearTask()

            notification.setIdle().post()
            runTask()
        }

        override fun onError(apiError: ApiError) {
            lastEventTime = System.currentTimeMillis()
            d(TAG, "Task $taskRunningId threw an error - $apiError")
            apiError.profileId = taskProfileId
            EventBus.getDefault().postSticky(ApiTaskErrorEvent(apiError))
            errorList.add(apiError)
            apiError.throwable?.printStackTrace()
            if (apiError.isCritical) {
                taskRunning?.cancel()
                notification.setCriticalError().post()
                clearTask()
                runTask()
            }
            else {
                notification.addError().post()
            }
        }

        override fun onProgress(step: Float) {
            lastEventTime = System.currentTimeMillis()
            if (step <= 0)
                return
            if (taskProgress < 0)
                taskProgress = 0f
            taskProgress += step
            taskProgress = min(100f, taskProgress)
            d(TAG, "Task $taskRunningId progress: ${taskProgress.roundToInt()}%")
            EventBus.getDefault().post(ApiTaskProgressEvent(taskProfileId, taskProgress, taskProgressText))
            notification.setProgress(taskProgress).post()
        }

        override fun onStartProgress(stringRes: Int) {
            lastEventTime = System.currentTimeMillis()
            taskProgressText = getString(stringRes)
            d(TAG, "Task $taskRunningId progress: $taskProgressText")
            EventBus.getDefault().post(ApiTaskProgressEvent(taskProfileId, taskProgress, taskProgressText))
            notification.setProgressText(taskProgressText).post()
        }
    }

    /*    _______        _                               _   _
         |__   __|      | |                             | | (_)
            | | __ _ ___| | __   _____  _____  ___ _   _| |_ _  ___  _ __
            | |/ _` / __| |/ /  / _ \ \/ / _ \/ __| | | | __| |/ _ \| '_ \
            | | (_| \__ \   <  |  __/>  <  __/ (__| |_| | |_| | (_) | | | |
            |_|\__,_|___/_|\_\  \___/_/\_\___|\___|\__,_|\__|_|\___/|_| |*/
    private fun runTask() {
        checkIfTaskFrozen()
        if (taskIsRunning)
            return
        if (taskCancelled || serviceClosed || (taskQueue.isEmpty() && finishingTaskQueue.isEmpty())) {
            serviceClosed = false
            allCompleted()
            return
        }

        lastEventTime = System.currentTimeMillis()

        val task = if (taskQueue.isEmpty()) finishingTaskQueue.removeAt(0) else taskQueue.removeAt(0)
        task.taskId = ++taskMaximumId
        task.prepare(app)
        taskIsRunning = true
        taskRunningId = task.taskId
        taskRunning = task
        taskProfileId = task.profileId
        taskProgress = -1f
        taskProgressText = task.taskName

        d(TAG, "Executing task $taskRunningId ($taskProgressText) - $task")

        // update the notification
        notification.setCurrentTask(taskRunningId, taskProgressText).post()

        // post an event
        EventBus.getDefault().post(ApiTaskStartedEvent(taskProfileId, task.profile))

        task.profile?.let { syncingProfiles.add(it) }

        try {
            when (task) {
                is EdziennikTask -> task.run(app, taskCallback)
                is NotifyTask -> task.run(app, taskCallback)
                is ErrorReportTask -> task.run(app, taskCallback, notification, errorList)
                is SzkolnyTask -> task.run(app, taskCallback)
            }
        } catch (e: Exception) {
            taskCallback.onError(ApiError(TAG, EXCEPTION_API_TASK).withThrowable(e))
        }
    }

    /**
     * Check if a task is inactive for more than 30 seconds.
     * If the user tries to cancel a task with no success at least three times,
     * consider it frozen as well.
     *
     * This usually means it is broken and won't become active again.
     * This method cancels the task and removes any pointers to it.
     */
    private fun checkIfTaskFrozen(): Boolean {
        if (System.currentTimeMillis() - lastEventTime > 30*1000
                || taskCancelTries >= 3) {
            val time = System.currentTimeMillis() - lastEventTime
            d(TAG, "!!! Task $taskRunningId froze for $time ms. $taskRunning")
            clearTask()
            return true
        }
        return false
    }

    /**
     * Stops the service if the current task is frozen/broken.
     */
    private fun stopIfTaskFrozen() {
        if (checkIfTaskFrozen()) {
            allCompleted()
        }
    }

    /**
     * Remove any task descriptors or pointers from the service.
     */
    private fun clearTask() {
        taskIsRunning = false
        taskRunningId = -1
        taskRunning = null
        taskProfileId = -1
        taskProgress = -1f
        taskProgressText = null
        taskCancelled = false
        taskCancelTries = 0
    }

    private fun allCompleted() {
        EventBus.getDefault().postSticky(ApiTaskAllFinishedEvent())
        stopSelf()
    }

    /*    ______               _   ____
         |  ____|             | | |  _ \
         | |____   _____ _ __ | |_| |_) |_   _ ___
         |  __\ \ / / _ \ '_ \| __|  _ <| | | / __|
         | |___\ V /  __/ | | | |_| |_) | |_| \__ \
         |______\_/ \___|_| |_|\__|____/ \__,_|__*/
    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    fun onApiTask(task: IApiTask) {
        EventBus.getDefault().removeStickyEvent(task)
        d(TAG, task.toString())

        // fix for duplicated tasks, thank you EventBus
        if (task in allTaskList)
            return
        allTaskList += task

        if (task is EdziennikTask) {
            when (task.request) {
                is EdziennikTask.SyncRequest -> app.db.profileDao().idsForSyncNow.forEach {
                    taskQueue += EdziennikTask.syncProfile(it)
                }
                is EdziennikTask.SyncProfileListRequest -> task.request.profileList.forEach {
                    taskQueue += EdziennikTask.syncProfile(it)
                }
                else -> {
                    taskQueue += task
                }
            }
        }
        else {
            taskQueue += task
        }
        d(TAG, "EventBus received an IApiTask: $task")
        d(TAG, "Current queue:")
        taskQueue.forEach {
            d(TAG, "  - $it")
        }
        runTask()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    fun onTaskCancelRequest(request: TaskCancelRequest) {
        EventBus.getDefault().removeStickyEvent(request)
        d(TAG, request.toString())

        taskCancelTries++
        taskCancelled = true
        taskRunning?.cancel()
        stopIfTaskFrozen()
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    fun onServiceCloseRequest(request: ServiceCloseRequest) {
        EventBus.getDefault().removeStickyEvent(request)
        d(TAG, request.toString())

        serviceClosed = true
        taskCancelled = true
        taskRunning?.cancel()
        allCompleted()
    }

    /*     _____                 _                                     _     _
          / ____|               (_)                                   (_)   | |
         | (___   ___ _ ____   ___  ___ ___    _____   _____ _ __ _ __ _  __| | ___  ___
          \___ \ / _ \ '__\ \ / / |/ __/ _ \  / _ \ \ / / _ \ '__| '__| |/ _` |/ _ \/ __|
          ____) |  __/ |   \ V /| | (_|  __/ | (_) \ V /  __/ |  | |  | | (_| |  __/\__ \
         |_____/ \___|_|    \_/ |_|\___\___|  \___/ \_/ \___|_|  |_|  |_|\__,_|\___||__*/
    override fun onCreate() {
        d(TAG, "Service created")
        EventBus.getDefault().register(this)
        notification.setIdle().setCloseAction()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        d(TAG, "Foreground service onStartCommand")
        startForeground(EdziennikNotification.NOTIFICATION_ID, notification.notification)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
