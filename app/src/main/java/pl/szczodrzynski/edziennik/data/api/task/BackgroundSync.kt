package pl.szczodrzynski.edziennik.data.api.task

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskAllFinishedEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskErrorEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskFinishedEvent
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ext.toApiError
import android.util.Log
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Runs the e-register sync without the foreground [pl.szczodrzynski.edziennik.data.api.ApiService],
 * so that it can be executed from a background worker.
 *
 * It mirrors what the service does for a sync request:
 * syncs each profile one after another, then runs [SzkolnyTask],
 * which creates and posts the notifications about new data.
 */
class BackgroundSync(private val app: App) {
    companion object {
        private const val TAG = "BackgroundSync"
        /** Give up on a task that did not finish in this time. */
        private const val TASK_TIMEOUT_MS = 3 * 60 * 1000L
    }

    /**
     * @param profileIds profiles to sync, or null to sync every
     * profile with sync enabled
     */
    suspend fun run(profileIds: List<Int>?) = withContext(Dispatchers.IO) {
        val ids = profileIds ?: app.db.profileDao().idsForSyncNow
        d(TAG, "Background sync of profiles $ids")

        val syncedProfiles = mutableListOf<Profile>()
        for (profileId in ids) {
            val task = EdziennikTask.syncProfile(profileId)
            task.prepare(app)
            task.profile?.let { syncedProfiles += it }
            runTask(task) { callback -> task.run(app, callback) }
        }

        val szkolnyTask = SzkolnyTask(app, syncedProfiles)
        szkolnyTask.prepare(app)
        runTask(szkolnyTask) { callback -> szkolnyTask.run(callback) }

        EventBus.getDefault().postSticky(ApiTaskAllFinishedEvent())
        d(TAG, "Background sync finished")
    }

    /**
     * Run a single task and wait until it completes,
     * fails with a critical error or times out.
     */
    private suspend fun runTask(task: IApiTask, block: (EdziennikCallback) -> Unit) {
        val profileId = task.profileId
        val startTime = System.currentTimeMillis()
        d(TAG, "Executing task ${task::class.java.simpleName} (profile $profileId)")

        val finished = withTimeoutOrNull(TASK_TIMEOUT_MS) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val isDone = AtomicBoolean(false)
                fun finish() {
                    if (isDone.compareAndSet(false, true) && continuation.isActive)
                        continuation.resume(Unit)
                }

                val callback = object : EdziennikCallback {
                    override fun onCompleted() {
                        EventBus.getDefault().postSticky(ApiTaskFinishedEvent(profileId))
                        finish()
                    }

                    override fun onRequiresUserAction(event: UserActionRequiredEvent) {
                        app.userActionManager.sendToUser(event)
                        task.cancel()
                        finish()
                    }

                    override fun onError(apiError: ApiError) {
                        apiError.profileId = profileId
                        Log.e(TAG, "Task error: $apiError", apiError.throwable)
                        EventBus.getDefault().postSticky(ApiTaskErrorEvent(apiError))
                        if (apiError.isCritical) {
                            task.cancel()
                            finish()
                        }
                    }

                    override fun onProgress(step: Float) {}
                    override fun onStartProgress(stringRes: Int) {}
                }

                continuation.invokeOnCancellation { task.cancel() }

                try {
                    block(callback)
                } catch (e: Exception) {
                    callback.onError(e.toApiError(TAG))
                    finish()
                }
            }
        }

        if (finished == null) {
            d(TAG, "Task ${task::class.java.simpleName} (profile $profileId) timed out")
            task.cancel()
        } else {
            d(TAG, "Task ${task::class.java.simpleName} (profile $profileId) finished in ${System.currentTimeMillis() - startTime} ms")
        }
    }
}
