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
import pl.szczodrzynski.edziennik.data.api.szkolny.request.EventShareRequest
import pl.szczodrzynski.edziennik.data.api.szkolny.request.ServerSyncRequest
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull
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

    fun getEvents(profiles: List<ProfileFull>): List<EventFull> {
        val teams = app.db.teamDao().allNow
        val notifications = app.db.notificationDao().getNotPostedNow()

        val response = api.serverSync(ServerSyncRequest(
                deviceId = app.deviceId,
                device = ServerSyncRequest.Device(
                        osType = "Android",
                        osVersion = Build.VERSION.RELEASE,
                        hardware = "${Build.MANUFACTURER} ${Build.MODEL}",
                        pushToken = app.config.sync.tokenApp,
                        appVersion = BuildConfig.VERSION_NAME,
                        appType = BuildConfig.BUILD_TYPE,
                        appVersionCode = BuildConfig.VERSION_CODE,
                        syncInterval = app.config.sync.interval
                ),
                userCodes = profiles.map { it.usernameId },
                users = profiles.map { profile ->
                    ServerSyncRequest.User(
                            profile.usernameId,
                            profile.studentNameLong ?: "",
                            profile.studentNameShort ?: "",
                            profile.loginStoreType,
                            teams.filter { it.profileId == profile.id }.map { it.code }
                    )
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

                    if (profile?.usernameId == event.sharedBy) sharedBy = "self"
                })
            }
        }

        return events
    }

    fun shareEvent(event: EventFull) {
        val team = app.db.teamDao().getByIdNow(event.profileId, event.teamId)

        api.shareEvent(EventShareRequest(
                deviceId = app.deviceId,
                sharedByName = event.sharedByName,
                shareTeamCode = team.code,
                event = event
        )).execute()
    }

    fun unshareEvent(event: EventFull) {
        val team = app.db.teamDao().getByIdNow(event.profileId, event.teamId)

        api.shareEvent(EventShareRequest(
                deviceId = app.deviceId,
                sharedByName = event.sharedByName,
                unshareTeamCode = team.code,
                eventId = event.id
        )).execute()
    }
}
