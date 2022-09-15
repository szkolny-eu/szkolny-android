/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-20.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data

import android.os.Build
import androidx.core.util.set
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.body.MediaTypeUtils
import im.wangchao.mhttp.callback.JsonCallbackHandler
import io.github.wulkanowy.signer.hebe.getSignatureHeaders
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe.HebeFilterType
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LessonRange
import pl.szczodrzynski.edziennik.data.db.entity.Subject
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

open class VulcanHebe(open val data: DataVulcan, open val lastSync: Long?) {
    companion object {
        const val TAG = "VulcanHebe"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun getDateTime(
        json: JsonObject?,
        key: String,
        default: Long = System.currentTimeMillis()
    ): Long {
        val date = json.getJsonObject(key)
        return date.getLong("Timestamp") ?: return default
    }

    fun getDate(json: JsonObject?, key: String): Date? {
        val date = json.getJsonObject(key)
        return date.getString("Date")?.let { Date.fromY_m_d(it) }
    }

    fun getTeacherId(json: JsonObject?, key: String): Long? {
        val teacher = json.getJsonObject(key)
        val teacherId = teacher.getLong("Id") ?: return null
        if (data.teacherList[teacherId] == null) {
            data.teacherList[teacherId] = Teacher(
                data.profileId,
                teacherId,
                teacher.getString("Name") ?: "",
                teacher.getString("Surname") ?: ""
            )
        }
        return teacherId
    }

    fun getSubjectId(json: JsonObject?, key: String): Long? {
        val subject = json.getJsonObject(key)
        val subjectId = subject.getLong("Id") ?: return null
        if (data.subjectList[subjectId] == null) {
            data.subjectList[subjectId] = Subject(
                data.profileId,
                subjectId,
                subject.getString("Name") ?: "",
                subject.getString("Kod") ?: ""
            )
        }
        return subjectId
    }

    fun getTeamId(json: JsonObject?, key: String): Long {
        val team = json.getJsonObject(key)
        val teamId = team.getLong("Id")
        var teamName = team.getString("Shortcut")
            ?: team.getString("Name")
            ?: ""
        teamName = "${profile?.studentClassName ?: ""} $teamName"
        return data.getTeam(
            id = teamId,
            name = teamName,
            schoolCode = data.schoolCode ?: "",
            isTeamClass = false,
        ).id
    }

    fun getLessonRange(json: JsonObject?, key: String): LessonRange? {
        val timeslot = json.getJsonObject(key)
        val position = timeslot.getInt("Position") ?: return null
        val start = timeslot.getString("Start") ?: return null
        val end = timeslot.getString("End") ?: return null
        val lessonRange = LessonRange(
            data.profileId,
            position,
            Time.fromH_m(start),
            Time.fromH_m(end)
        )
        data.lessonRanges[position] = lessonRange
        return lessonRange
    }

    fun getSemester(json: JsonObject?): Int {
        val periodId = json.getInt("PeriodId") ?: return 1
        return if (periodId == data.semester1Id)
            1
        else
            2
    }

    fun isCurrentYear(date: Date): Boolean {
        return profile?.let { profile ->
            return@let date >= profile.dateSemester1Start
        } ?: false
    }

    fun isCurrentYear(dateTime: Long): Boolean {
        return profile?.let { profile ->
            return@let dateTime >= profile.dateSemester1Start.inMillis - 604800000
        } ?: false
    }

    inline fun <reified T> apiRequest(
        tag: String,
        endpoint: String,
        method: Int = GET,
        payload: JsonElement? = null,
        baseUrl: Boolean = false,
        firebaseToken: String? = null,
        crossinline onSuccess: (json: T, response: Response?) -> Unit
    ) {
        val url = "${if (baseUrl) data.apiUrl else data.fullApiUrl}$endpoint"

        d(tag, "Request: Vulcan/Hebe - $url")

        val privateKey = data.hebePrivateKey
        val publicHash = data.hebePublicHash

        if (privateKey == null || publicHash == null) {
            data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            return
        }

        val timestamp = ZonedDateTime.now(ZoneId.of("GMT"))
        val timestampMillis = timestamp.toInstant().toEpochMilli()
        val timestampIso = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"))

        val finalPayload = if (payload != null) {
            JsonObject(
                "AppName" to VULCAN_HEBE_APP_NAME,
                "AppVersion" to VULCAN_HEBE_APP_VERSION,
                "CertificateId" to publicHash,
                "Envelope" to payload,
                "FirebaseToken" to (firebaseToken ?: data.app.config.sync.tokenVulcanHebe),
                "API" to 1,
                "RequestId" to UUID.randomUUID().toString(),
                "Timestamp" to timestampMillis,
                "TimestampFormatted" to timestampIso
            )
        } else null
        val jsonString = finalPayload?.toString()

        val headers = getSignatureHeaders(
            publicHash,
            privateKey,
            jsonString,
            endpoint,
            timestamp
        )

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null) {
                    data.error(
                        ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response)
                    )
                    return
                }

                val status = json.getJsonObject("Status")
                val statusCode = status?.getInt("Code") ?: 0
                if (statusCode != 0) {
                    val statusMessage = status?.getString("Message")
                    val errorCode = when (statusCode) {
                        -1 -> ERROR_VULCAN_HEBE_SERVER_ERROR
                        100 -> ERROR_VULCAN_HEBE_SIGNATURE_ERROR
                        101 -> ERROR_VULCAN_HEBE_INVALID_PAYLOAD
                        106 -> ERROR_VULCAN_HEBE_FIREBASE_ERROR
                        108 -> ERROR_VULCAN_HEBE_CERTIFICATE_GONE
                        200 -> when (true) {
                            statusMessage?.contains("Class") -> ERROR_LOGIN_VULCAN_NO_PUPILS
                            statusMessage?.contains("Token") -> ERROR_LOGIN_VULCAN_INVALID_TOKEN
                            else -> ERROR_VULCAN_HEBE_ENTITY_NOT_FOUND
                        }
                        201 -> ERROR_LOGIN_VULCAN_EXPIRED_TOKEN
                        203 -> when (json.getJsonObject("Envelope").getInt("AvailableRetries")) {
                            2 -> ERROR_LOGIN_VULCAN_INVALID_PIN_2_REMAINING
                            1 -> ERROR_LOGIN_VULCAN_INVALID_PIN_1_REMAINING
                            else -> ERROR_LOGIN_VULCAN_INVALID_PIN_0_REMAINING
                        }
                        204 -> ERROR_LOGIN_VULCAN_INVALID_PIN_0_REMAINING
                        else -> ERROR_VULCAN_HEBE_OTHER
                    }
                    data.error(
                        ApiError(tag, errorCode)
                            .withResponse(response)
                            .withApiResponse(json.toString())
                    )
                    return
                }

                val envelope = if (json.get("Envelope").isJsonNull && null is T)
                    null as T
                else when (T::class.java) {
                    JsonObject::class.java -> json.getJsonObject("Envelope") as T
                    JsonArray::class.java -> json.getJsonArray("Envelope") as T
                    java.lang.Boolean::class.java -> json.getBoolean("Envelope") as T
                    else -> {
                        data.error(
                            ApiError(tag, ERROR_RESPONSE_EMPTY)
                                .withResponse(response)
                                .withApiResponse(json)
                        )
                        return
                    }
                }

                try {
                    onSuccess(envelope, response)
                } catch (e: Exception) {
                    data.error(
                        ApiError(tag, EXCEPTION_VULCAN_HEBE_REQUEST)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(json)
                    )
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(
                    ApiError(tag, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable)
                )
            }
        }

        Request.builder()
            .url(url)
            .userAgent(VULCAN_HEBE_USER_AGENT)
            .addHeader("vOS", "Android")
            .addHeader("vDeviceModel", Build.MODEL)
            .addHeader("vAPI", "1")
            .apply {
                if (data.hebeContext != null)
                    addHeader("vContext", data.hebeContext)
                headers.forEach {
                    addHeader(it.key, it.value)
                }
                when (method) {
                    GET -> get()
                    POST -> {
                        post()
                        setTextBody(jsonString, MediaTypeUtils.APPLICATION_JSON)
                    }
                }
            }
            .allowErrorCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .allowErrorCode(HttpURLConnection.HTTP_FORBIDDEN)
            .allowErrorCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .allowErrorCode(HttpURLConnection.HTTP_UNAVAILABLE)
            .callback(callback)
            .build()
            .enqueue()
    }

    inline fun <reified T> apiGet(
        tag: String,
        endpoint: String,
        query: Map<String, String> = mapOf(),
        baseUrl: Boolean = false,
        firebaseToken: String? = null,
        crossinline onSuccess: (json: T, response: Response?) -> Unit
    ) {
        val queryPath = query.map {
            it.key + "=" + URLEncoder.encode(it.value, "UTF-8").replace("+", "%20")
        }.join("&")
        apiRequest(
            tag,
            if (query.isNotEmpty()) "$endpoint?$queryPath" else endpoint,
            baseUrl = baseUrl,
            firebaseToken = firebaseToken,
            onSuccess = onSuccess
        )
    }

    inline fun <reified T> apiPost(
        tag: String,
        endpoint: String,
        payload: JsonElement,
        baseUrl: Boolean = false,
        firebaseToken: String? = null,
        crossinline onSuccess: (json: T, response: Response?) -> Unit
    ) {
        apiRequest(
            tag,
            endpoint,
            method = POST,
            payload,
            baseUrl = baseUrl,
            firebaseToken = firebaseToken,
            onSuccess = onSuccess
        )
    }

    fun apiGetList(
        tag: String,
        endpoint: String,
        filterType: HebeFilterType? = null,
        dateFrom: Date? = null,
        dateTo: Date? = null,
        lastSync: Long? = null,
        folder: Int? = null,
        message_box: String? = null,
        params: Map<String, String> = mapOf(),
        includeFilterType: Boolean = true,
        onSuccess: (data: List<JsonObject>, response: Response?) -> Unit
    ) {
        val url = if (includeFilterType && filterType != null)
            "$endpoint/${filterType.endpoint}"
        else endpoint

        val query = params.toMutableMap()

        when (filterType) {
            HebeFilterType.BY_PUPIL -> {
                query["unitId"] = data.studentUnitId.toString()
                query["pupilId"] = data.studentId.toString()
                query["periodId"] = data.studentSemesterId.toString()
            }
            HebeFilterType.BY_PERSON -> {
                query["loginId"] = data.studentLoginId.toString()
            }
            HebeFilterType.BY_PERIOD -> {
                query["periodId"] = data.studentSemesterId.toString()
                query["pupilId"] = data.studentId.toString()
            }
            HebeFilterType.BY_MESSAGEBOX -> {
                if (message_box == null) {
                    throw IllegalArgumentException("message_box cannot be null")
                }
                query["box"] = message_box
            }
            else -> {}
        }

        if (dateFrom != null)
            query["dateFrom"] = dateFrom.stringY_m_d
        if (dateTo != null)
            query["dateTo"] = dateTo.stringY_m_d

        if (folder != null)
            query["folder"] = folder.toString()

        val semester1Start = profile?.dateSemester1Start?.inMillis

        query["lastId"] = "-2147483648" // don't ask, it's just Vulcan
        query["pageSize"] = "500"
        query["lastSyncDate"] = LocalDateTime
            .ofInstant(
                Instant.ofEpochMilli(lastSync ?: semester1Start ?: 0),
                ZoneId.systemDefault()
            )
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        apiGet(tag, url, query) { json: JsonArray, response ->
            onSuccess(json.map { it.asJsonObject }, response)
        }
    }
}
