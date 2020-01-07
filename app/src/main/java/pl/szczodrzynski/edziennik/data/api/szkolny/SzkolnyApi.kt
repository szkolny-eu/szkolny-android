/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.data.api.szkolny

import android.os.Build
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.data.api.szkolny.adapter.DateAdapter
import pl.szczodrzynski.edziennik.data.api.szkolny.adapter.TimeAdapter
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.SignatureInterceptor
import pl.szczodrzynski.edziennik.data.api.szkolny.request.ErrorReportRequest
import pl.szczodrzynski.edziennik.data.api.szkolny.request.EventShareRequest
import pl.szczodrzynski.edziennik.data.api.szkolny.request.ServerSyncRequest
import pl.szczodrzynski.edziennik.data.api.szkolny.request.WebPushRequest
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ApiResponse
import pl.szczodrzynski.edziennik.data.api.szkolny.response.WebPushResponse
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.md5
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit.SECONDS

class SzkolnyApi(val app: App) {

    private val api: SzkolnyService

    init {
        val okHttpClient: OkHttpClient = app.http.newBuilder()
                .followRedirects(true)
                .callTimeout(30, SECONDS)
                .addInterceptor(SignatureInterceptor(app))
                .build()

        val gsonConverterFactory = GsonConverterFactory.create(
                GsonBuilder()
                        .setLenient()
                        .registerTypeAdapter(Date::class.java, DateAdapter())
                        .registerTypeAdapter(Time::class.java, TimeAdapter())
                        .create())

        val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl("https://api.szkolny.eu/")
                .addConverterFactory(gsonConverterFactory)
                .client(okHttpClient)
                .build()

        api = retrofit.create()
    }

    fun getEvents(profiles: List<Profile>): List<EventFull> {
        val teams = app.db.teamDao().allNow
        val notifications = app.db.notificationDao().getNotPostedNow()

        val response = api.serverSync(ServerSyncRequest(
                deviceId = app.deviceId,
                device = run {
                    val config = app.config
                    val device = ServerSyncRequest.Device(
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
                },
                userCodes = profiles.map { it.userCode },
                users = profiles.mapNotNull { profile ->
                    val config = app.config.getFor(profile.id)
                    val user = ServerSyncRequest.User(
                            profile.userCode,
                            profile.studentNameLong ?: "",
                            profile.studentNameShort ?: "",
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
        )).execute().body()

        val events = mutableListOf<EventFull>()

        response?.data?.events?.forEach { event ->
            teams.filter { it.code == event.teamCode }.forEach { team ->
                val profile = profiles.firstOrNull { it.id == team.profileId }

                events.add(event.apply {
                    profileId = team.profileId
                    teamId = team.id
                    addedManually = true
                    seen = profile?.empty ?: false
                    notified = profile?.empty ?: false

                    if (profile?.userCode == event.sharedBy) sharedBy = "self"
                })
            }
        }

        return events
    }

    fun shareEvent(event: EventFull): ApiResponse<Nothing>? {
        val team = app.db.teamDao().getByIdNow(event.profileId, event.teamId)

        return api.shareEvent(EventShareRequest(
                deviceId = app.deviceId,
                sharedByName = event.sharedByName,
                shareTeamCode = team.code,
                event = event
        )).execute().body()
    }

    fun unshareEvent(event: Event): ApiResponse<Nothing>? {
        val team = app.db.teamDao().getByIdNow(event.profileId, event.teamId)

        return api.shareEvent(EventShareRequest(
                deviceId = app.deviceId,
                sharedByName = event.sharedByName,
                unshareTeamCode = team.code,
                eventId = event.id
        )).execute().body()
    }

    /*fun eventEditRequest(requesterName: String, event: Event): ApiResponse<Nothing>? {

    }*/

    fun pairBrowser(browserId: String?, pairToken: String?, onError: ((List<ApiResponse.Error>) -> Unit)? = null): List<WebPushResponse.Browser> {
        val response = api.webPush(WebPushRequest(
                action = "pairBrowser",
                deviceId = app.deviceId,
                browserId = browserId,
                pairToken = pairToken
        )).execute().body()

        response?.errors?.let {
            onError?.invoke(it)
            return emptyList()
        }

        return response?.data?.browsers ?: emptyList()
    }

    fun listBrowsers(onError: ((List<ApiResponse.Error>) -> Unit)? = null): List<WebPushResponse.Browser> {
        val response = api.webPush(WebPushRequest(
                action = "listBrowsers",
                deviceId = app.deviceId
        )).execute().body()

        return response?.data?.browsers ?: emptyList()
    }

    fun unpairBrowser(browserId: String): List<WebPushResponse.Browser> {
        val response = api.webPush(WebPushRequest(
                action = "unpairBrowser",
                deviceId = app.deviceId,
                browserId = browserId
        )).execute().body()

        return response?.data?.browsers ?: emptyList()
    }

    fun errorReport(errors: List<ErrorReportRequest.Error>): ApiResponse<Nothing>? {
        return api.errorReport(ErrorReportRequest(
                deviceId = app.deviceId,
                errors = errors
        )).execute().body()
    }
}
