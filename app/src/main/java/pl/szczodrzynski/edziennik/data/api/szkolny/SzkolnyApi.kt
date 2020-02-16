/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.data.api.szkolny

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.szkolny.adapter.DateAdapter
import pl.szczodrzynski.edziennik.data.api.szkolny.adapter.TimeAdapter
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.SignatureInterceptor
import pl.szczodrzynski.edziennik.data.api.szkolny.request.*
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ApiResponse
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.data.api.szkolny.response.WebPushResponse
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.FeedbackMessage
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.md5
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorDetailsDialog
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorSnackbar
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.coroutines.CoroutineContext

class SzkolnyApi(val app: App) : CoroutineScope {
    companion object {
        const val TAG = "SzkolnyApi"
    }

    private val api: SzkolnyService
    private val retrofit: Retrofit

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    init {
        val okHttpClient: OkHttpClient = app.http.newBuilder()
                .followRedirects(true)
                .callTimeout(10, SECONDS)
                .addInterceptor(SignatureInterceptor(app))
                .build()

        val gsonConverterFactory = GsonConverterFactory.create(
                GsonBuilder()
                        .setLenient()
                        .registerTypeAdapter(Date::class.java, DateAdapter())
                        .registerTypeAdapter(Time::class.java, TimeAdapter())
                        .create())

        retrofit = Retrofit.Builder()
                .baseUrl("https://api.szkolny.eu/")
                .addConverterFactory(gsonConverterFactory)
                .client(okHttpClient)
                .build()

        api = retrofit.create()
    }

    suspend inline fun <T> runCatching(errorSnackbar: ErrorSnackbar, crossinline block: SzkolnyApi.() -> T?): T? {
        return try {
            withContext(Dispatchers.Default) { block() }
        }
        catch (e: Exception) {
            errorSnackbar.addError(ApiError.fromThrowable(TAG, e)).show()
            null
        }
    }
    suspend inline fun <T> runCatching(activity: AppCompatActivity, crossinline block: SzkolnyApi.() -> T?): T? {
        return try {
            withContext(Dispatchers.Default) { block() }
        }
        catch (e: Exception) {
            ErrorDetailsDialog(
                    activity,
                    listOf(ApiError.fromThrowable(TAG, e)),
                    R.string.error_occured
            )
            null
        }
    }
    inline fun <T> runCatching(block: SzkolnyApi.() -> T, onError: (e: Throwable) -> Unit): T? {
        return try {
            block()
        }
        catch (e: Exception) {
            onError(e)
            null
        }
    }

    /**
     * Check if a server request returned a successful response.
     *
     * If not, throw a [SzkolnyApiException] containing an [ApiResponse.Error],
     * or null if it's a HTTP call error.
     */
    @Throws(Exception::class)
    private inline fun <reified T> parseResponse(response: Response<ApiResponse<T>>): T {
        if (response.isSuccessful && response.body()?.success == true) {
            if (Unit is T) {
                return Unit
            }
            if (response.body()?.data != null) {
                return response.body()?.data!!
            }
        }

        val body = response.body() ?: response.errorBody()?.let {
            try {
                retrofit.responseBodyConverter<ApiResponse<T>>(ApiResponse::class.java, arrayOf()).convert(it)
            }
            catch (e: Exception) {
                null
            }
        }

        throw SzkolnyApiException(body?.errors?.firstOrNull())
    }

    @Throws(Exception::class)
    private fun getDevice() = run {
        val config = app.config
        val device = Device(
                osType = "Android",
                osVersion = Build.VERSION.RELEASE,
                hardware = "${Build.MANUFACTURER} ${Build.MODEL}",
                pushToken = app.config.sync.tokenApp,
                appVersion = BuildConfig.VERSION_NAME,
                appType = BuildConfig.BUILD_TYPE,
                appVersionCode = BuildConfig.VERSION_CODE,
                syncInterval = app.config.sync.interval
        )
        device.toString().md5().let {
            if (it == config.hash)
                null
            else {
                config.hash = it
                device
            }
        }
    }

    @Throws(Exception::class)
    fun getEvents(profiles: List<Profile>, notifications: List<Notification>, blacklistedIds: List<Long>): List<EventFull> {
        val teams = app.db.teamDao().allNow

        val response = api.serverSync(ServerSyncRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                userCodes = profiles.map { it.userCode },
                users = profiles.mapNotNull { profile ->
                    val config = app.config.getFor(profile.id)
                    val user = ServerSyncRequest.User(
                            profile.userCode,
                            profile.studentNameLong,
                            profile.studentNameShort,
                            profile.loginStoreType,
                            teams.filter { it.profileId == profile.id }.map { it.code }
                    )
                    user.toString().md5().let {
                        if (it == config.hash)
                            null
                        else {
                            config.hash = it
                            user
                        }
                    }
                },
                notifications = notifications.map { ServerSyncRequest.Notification(it.profileName ?: "", it.type, it.text) }
        )).execute()
        parseResponse(response)

        val events = mutableListOf<EventFull>()

        response.body()?.data?.events?.forEach { event ->
            if (event.id in blacklistedIds)
                return@forEach
            teams.filter { it.code == event.teamCode }.onEach { team ->
                val profile = profiles.firstOrNull { it.id == team.profileId } ?: return@onEach

                events.add(EventFull(event).apply {
                    profileId = team.profileId
                    teamId = team.id
                    addedManually = true
                    seen = profile.empty
                    notified = profile.empty

                    if (profile.userCode == event.sharedBy) sharedBy = "self"
                })
            }
        }

        return events
    }

    @Throws(Exception::class)
    fun shareEvent(event: EventFull) {
        val team = app.db.teamDao().getByIdNow(event.profileId, event.teamId)

        val response = api.shareEvent(EventShareRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                sharedByName = event.sharedByName,
                shareTeamCode = team.code,
                event = event
        )).execute()
        parseResponse(response)
    }

    @Throws(Exception::class)
    fun unshareEvent(event: Event) {
        val team = app.db.teamDao().getByIdNow(event.profileId, event.teamId)

        val response = api.shareEvent(EventShareRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                sharedByName = event.sharedByName,
                unshareTeamCode = team.code,
                eventId = event.id
        )).execute()
        parseResponse(response)
    }

    /*fun eventEditRequest(requesterName: String, event: Event): ApiResponse<Nothing>? {

    }*/

    @Throws(Exception::class)
    fun pairBrowser(browserId: String?, pairToken: String?): List<WebPushResponse.Browser> {
        val response = api.webPush(WebPushRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                action = "pairBrowser",
                browserId = browserId,
                pairToken = pairToken
        )).execute()
        parseResponse(response)

        return response.body()?.data?.browsers ?: emptyList()
    }

    @Throws(Exception::class)
    fun listBrowsers(): List<WebPushResponse.Browser> {
        val response = api.webPush(WebPushRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                action = "listBrowsers"
        )).execute()
        parseResponse(response)

        return response.body()?.data?.browsers ?: emptyList()
    }

    @Throws(Exception::class)
    fun unpairBrowser(browserId: String): List<WebPushResponse.Browser> {
        val response = api.webPush(WebPushRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                action = "unpairBrowser",
                browserId = browserId
        )).execute()
        parseResponse(response)

        return response.body()?.data?.browsers ?: emptyList()
    }

    @Throws(Exception::class)
    fun errorReport(errors: List<ErrorReportRequest.Error>) {
        val response = api.errorReport(ErrorReportRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                appVersion = BuildConfig.VERSION_NAME,
                errors = errors
        )).execute()
        parseResponse(response)
    }

    @Throws(Exception::class)
    fun unregisterAppUser(userCode: String) {
        val response = api.appUser(AppUserRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                userCode = userCode
        )).execute()
        parseResponse(response)
    }

    @Throws(Exception::class)
    fun getUpdate(channel: String): List<Update> {
        val response = api.updates(channel).execute()
        parseResponse(response)

        return response.body()?.data ?: emptyList()
    }

    @Throws(Exception::class)
    fun sendFeedbackMessage(senderName: String?, targetDeviceId: String?, text: String): FeedbackMessage {
        val response = api.feedbackMessage(FeedbackMessageRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                senderName = senderName,
                targetDeviceId = targetDeviceId,
                text = text
        )).execute()
        val data = parseResponse(response)

        return data.message
    }
}
