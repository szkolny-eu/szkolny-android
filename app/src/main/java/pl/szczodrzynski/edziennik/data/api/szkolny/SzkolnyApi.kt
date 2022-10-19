/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.data.api.szkolny

import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_API_INVALID_SIGNATURE
import pl.szczodrzynski.edziennik.data.api.szkolny.adapter.DateAdapter
import pl.szczodrzynski.edziennik.data.api.szkolny.adapter.TimeAdapter
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.ApiCacheInterceptor
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.SignatureInterceptor
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.Signing
import pl.szczodrzynski.edziennik.data.api.szkolny.request.*
import pl.szczodrzynski.edziennik.data.api.szkolny.response.*
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.ext.keys
import pl.szczodrzynski.edziennik.ext.md5
import pl.szczodrzynski.edziennik.ext.toApiError
import pl.szczodrzynski.edziennik.ext.toErrorCode
import pl.szczodrzynski.edziennik.ui.error.ErrorDetailsDialog
import pl.szczodrzynski.edziennik.ui.error.ErrorSnackbar
import pl.szczodrzynski.edziennik.ui.login.LoginInfo
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
                .addInterceptor(ApiCacheInterceptor(app))
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
            withContext(Dispatchers.Default) { block.invoke(this@SzkolnyApi) }
        }
        catch (e: Exception) {
            errorSnackbar.addError(e.toApiError(TAG)).show()
            null
        }
    }
    suspend inline fun <T> runCatching(activity: AppCompatActivity, crossinline block: SzkolnyApi.() -> T?): T? {
        return try {
            withContext(Dispatchers.Default) { block.invoke(this@SzkolnyApi) }
        }
        catch (e: Exception) {
            withContext(coroutineContext) {
                val apiError = e.toApiError(TAG)
                if (apiError.errorCode == ERROR_API_INVALID_SIGNATURE) {
                    Toast.makeText(activity, R.string.error_no_api_access, Toast.LENGTH_SHORT).show()
                    return@withContext null
                }
                ErrorDetailsDialog(
                    activity,
                    listOf(apiError),
                    R.string.error_occured
                ).show()
                null
            }
            null
        }
    }
    inline fun <T> runCatching(block: SzkolnyApi.() -> T, onError: (e: Throwable) -> Unit): T? {
        return try {
            block.invoke(this@SzkolnyApi)
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
    private inline fun <reified T> parseResponse(
        response: Response<ApiResponse<T>>,
        updateDeviceHash: Boolean = false,
    ): T {
        app.config.update = response.body()?.update?.let { update ->
            if (update.versionCode > BuildConfig.VERSION_CODE) {
                if (update.updateMandatory
                        && EventBus.getDefault().hasSubscriberForEvent(update::class.java)) {
                    EventBus.getDefault().postSticky(update)
                }
                update
            }
            else
                null
        }

        response.body()?.registerAvailability?.let { registerAvailability ->
            app.config.sync.registerAvailability = registerAvailability
        }

        if (response.isSuccessful && response.body()?.success == true) {
            // update the device's hash on success
            if (updateDeviceHash) {
                val hash = getDevice()?.toString()?.md5()
                if (hash != null) {
                    app.config.hash = hash
                }
            }

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

        if (body?.errors?.any { it.toErrorCode() == ERROR_API_INVALID_SIGNATURE } == true) {
            app.config.apiInvalidCert = Signing.appCertificate.md5()
        }

        throw SzkolnyApiException(body?.errors?.firstOrNull())
    }

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
        val hash = device.toString().md5()
        if (hash == config.hash)
            return@run null
        return@run device
    }

    @Throws(Exception::class)
    fun getEvents(
        profiles: List<Profile>,
        notifications: List<Notification>,
        blacklistedIds: List<Long>,
        lastSyncTime: Long,
    ): Pair<List<EventFull>, List<Note>> {
        val teams = app.db.teamDao().allNow

        val users = profiles.mapNotNull { profile ->
            val config = app.config.getFor(profile.id)
            val user = ServerSyncRequest.User(
                profile.userCode,
                profile.studentNameLong,
                profile.studentNameShort,
                profile.loginStoreType.id,
                teams.filter { it.profileId == profile.id }.map { it.code }
            )
            val hash = user.toString().md5()
            if (hash == config.hash)
                return@mapNotNull null
            return@mapNotNull user to config
        }

        val response = api.serverSync(ServerSyncRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                userCodes = profiles.map { it.userCode },
                users = users.keys(),
                lastSync = lastSyncTime,
                notifications = notifications.map { ServerSyncRequest.Notification(it.profileName ?: "", it.type, it.text) }
        )).execute()
        val (events, notes, hasBrowsers) = parseResponse(response, updateDeviceHash = true)

        hasBrowsers?.let {
            app.config.sync.webPushEnabled = it
        }

        // update users' hashes on success
        users.forEach { (user, config) ->
            config.hash = user.toString().md5()
        }

        val eventList = mutableListOf<EventFull>()
        val noteList = mutableListOf<Note>()

        events.forEach { event ->
            // skip blacklisted events
            if (event.id in blacklistedIds)
                return@forEach

            // force nullable non-negative colors
            if (event.color == -1)
                event.color = null

            val eventSharedBy = event.sharedBy

            // create the event for every matching team and profile
            teams.filter { it.code == event.teamCode }.onEach { team ->
                val profile = profiles.firstOrNull { it.id == team.profileId } ?: return@onEach
                if (!profile.canShare)
                    return@forEach

                eventList += EventFull(event).apply {
                    profileId = team.profileId
                    teamId = team.id
                    addedManually = true
                    seen = profile.empty
                    notified = profile.empty

                    if (profile.userCode == event.sharedBy) {
                        sharedBy = "self"
                        addedManually = true
                    } else {
                        sharedBy = eventSharedBy
                    }
                }
            }
        }

        notes.forEach { note ->
            val noteSharedBy = note.sharedBy

            // create the note for every matching team and profile
            teams.filter { it.code == note.teamCode }.onEach { team ->
                val profile = profiles.firstOrNull { it.id == team.profileId } ?: return@onEach
                if (!profile.canShare)
                    return@forEach
                note.profileId = team.profileId
                if (profile.userCode == note.sharedBy) {
                    note.sharedBy = "self"
                } else {
                    note.sharedBy = noteSharedBy
                }

                if (app.noteManager.hasValidOwner(note))
                    noteList += note
            }
        }
        return eventList to noteList
    }

    @Throws(Exception::class)
    fun shareEvent(event: EventFull) {
        val profile = app.db.profileDao().getByIdNow(event.profileId)
            ?: throw NullPointerException("Profile is not found")
        val team = app.db.teamDao().getByIdNow(event.profileId, event.teamId)
            ?: throw NullPointerException("Team is not found")

        val response = api.shareEvent(EventShareRequest(
            deviceId = app.deviceId,
            device = getDevice(),
            userCode = profile.userCode,
            studentNameLong = profile.studentNameLong,
            shareTeamCode = team.code,
            event = event
        )).execute()
        parseResponse(response, updateDeviceHash = true)
    }

    @Throws(Exception::class)
    fun unshareEvent(event: Event) {
        val profile = app.db.profileDao().getByIdNow(event.profileId)
            ?: throw NullPointerException("Profile is not found")
        val team = app.db.teamDao().getByIdNow(event.profileId, event.teamId)
            ?: throw NullPointerException("Team is not found")

        val response = api.shareEvent(EventShareRequest(
            deviceId = app.deviceId,
            device = getDevice(),
            userCode = profile.userCode,
            studentNameLong = profile.studentNameLong,
            unshareTeamCode = team.code,
            eventId = event.id
        )).execute()
        parseResponse(response, updateDeviceHash = true)
    }

    @Throws(Exception::class)
    fun shareNote(note: Note) {
        val profile = app.db.profileDao().getByIdNow(note.profileId)
            ?: throw NullPointerException("Profile is not found")
        val team = app.db.teamDao().getClassNow(note.profileId)
            ?: throw NullPointerException("TeamClass is not found")

        val response = api.shareNote(NoteShareRequest(
            deviceId = app.deviceId,
            device = getDevice(),
            userCode = profile.userCode,
            studentNameLong = profile.studentNameLong,
            shareTeamCode = team.code,
            note = note,
        )).execute()
        parseResponse(response)
    }

    @Throws(Exception::class)
    fun unshareNote(note: Note) {
        val profile = app.db.profileDao().getByIdNow(note.profileId)
            ?: throw NullPointerException("Profile is not found")
        val team = app.db.teamDao().getClassNow(note.profileId)
            ?: throw NullPointerException("TeamClass is not found")

        val response = api.shareNote(NoteShareRequest(
            deviceId = app.deviceId,
            device = getDevice(),
            userCode = profile.userCode,
            studentNameLong = profile.studentNameLong,
            unshareTeamCode = team.code,
            noteId = note.id,
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

        return parseResponse(response, updateDeviceHash = true).browsers
    }

    @Throws(Exception::class)
    fun listBrowsers(): List<WebPushResponse.Browser> {
        val response = api.webPush(WebPushRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                action = "listBrowsers"
        )).execute()

        return parseResponse(response, updateDeviceHash = true).browsers
    }

    @Throws(Exception::class)
    fun unpairBrowser(browserId: String): List<WebPushResponse.Browser> {
        val response = api.webPush(WebPushRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                action = "unpairBrowser",
                browserId = browserId
        )).execute()

        return parseResponse(response, updateDeviceHash = true).browsers
    }

    @Throws(Exception::class)
    fun errorReport(errors: List<ErrorReportRequest.Error>) {
        val response = api.errorReport(ErrorReportRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                appVersion = BuildConfig.VERSION_NAME,
                errors = errors
        )).execute()
        parseResponse(response, updateDeviceHash = true)
    }

    @Throws(Exception::class)
    fun unregisterAppUser(userCode: String) {
        val response = api.appUser(AppUserRequest(
                deviceId = app.deviceId,
                device = getDevice(),
                userCode = userCode
        )).execute()
        parseResponse(response, updateDeviceHash = true)
    }

    @Throws(Exception::class)
    fun getUpdate(channel: String): List<Update> {
        val response = api.updates(channel).execute()
        return parseResponse(response)
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

        return parseResponse(response, updateDeviceHash = true).message
    }

    @Throws(Exception::class)
    fun getRealms(registerName: String): List<LoginInfo.Platform> {
        val response = api.fsLoginRealms(registerName).execute()
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        }
        throw SzkolnyApiException(null)
    }

    @Throws(Exception::class)
    fun getContributors(): ContributorsResponse {
        val response = api.contributors().execute()
        if (response.isSuccessful && response.body() != null) {
            return parseResponse(response)
        }
        throw SzkolnyApiException(null)
    }

    @Throws(Exception::class)
    fun getFirebaseToken(registerName: String): String {
        val response = api.firebaseToken(registerName).execute()
        return parseResponse(response)
    }

    @Throws(Exception::class)
    fun getRegisterAvailability(): Map<String, RegisterAvailabilityStatus> {
        val response = api.registerAvailability().execute()
        return parseResponse(response)
    }
}
