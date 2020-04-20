/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.content.Intent
import android.util.Log
import com.google.firebase.iid.zzad
import com.google.firebase.iid.zzaz
import com.google.firebase.messaging.zzc
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.*
import java.util.*
import com.google.firebase.messaging.zzo.zza as logNotificationOpen
import com.google.firebase.messaging.zzo.zza as logNotificationReceived
import com.google.firebase.messaging.zzo.zzb as logNotificationDismiss
import com.google.firebase.messaging.zzo.zzd as shouldUploadMetrics

@SuppressLint("Registered")
open class FirebaseService : zzc() {
    companion object {
        private const val TAG = "FirebaseService"
    }

    private val messageQueue = ArrayDeque<String>(10)

    open fun onMessageReceived(message: Message) = Unit
    open fun onDeletedMessages() = Unit
    open fun onMessageSent(messageId: String?) = Unit
    open fun onSendError(messageId: String?, exception: Exception) = Unit
    open fun onNewToken(token: String?) = Unit

    // apparently this gets the correct intent from some
    // kind of queue inside Firebase's InstanceID Receiver
    final override fun zza(intent: Intent?) = zzaz.zza()?.zzb()
    final override fun zzb(intent: Intent?): Boolean {
        val action = intent?.action
        if (action == "com.google.firebase.messaging.NOTIFICATION_OPEN") {
            intent.getParcelableExtra<PendingIntent>("pending_intent")?.let {
                try {
                    it.send()
                } catch (e: CanceledException) {
                    Log.e(TAG, "Notification pending intent canceled")
                }
            }

            if (shouldUploadMetrics(intent)) {
                logNotificationOpen(intent)
            }

            return true
        }
        return false
    }
    final override fun zzc(intent: Intent?) {
        val action = intent?.action
        val json = intent?.toJsonObject()
        Log.d(TAG, "zzc Intent(action=$action, extras=$json)")
        if (action == null || json == null)
            return

        when (action) {
            "com.google.firebase.messaging.NOTIFICATION_DISMISS" -> {
                if (shouldUploadMetrics(intent)) {
                    logNotificationDismiss(intent)
                }
            }
            "com.google.firebase.messaging.NEW_TOKEN" -> {
                onNewToken(json.getString("token"))
            }
            "com.google.android.c2dm.intent.RECEIVE",
            "com.google.firebase.messaging.RECEIVE_DIRECT_BOOT" -> {
                val messageId = json.getString("google.message_id")
                if (messageId != null) {
                    // send back an acknowledgement to Google Play Services
                    val ackBundle = Bundle(
                            "google.message_id" to messageId
                    )
                    zzad.zza(this).zza(2, ackBundle)
                }
                // check for duplicate message
                // and add it to queue
                if (messageId.isNotNullNorEmpty()) {
                    if (messageQueue.contains(messageId)) {
                        Log.d(TAG, "Received duplicate message: $messageId")
                        return
                    }
                    if (messageQueue.size >= 10)
                        messageQueue.remove()
                    messageQueue += messageId
                }
                // process the received message
                processMessage(messageId, json, intent)
            }
            else -> {
                Log.d(TAG, "Unknown intent action: $action")
            }
        }
    }

    private fun processMessage(messageId: String?, json: JsonObject, intent: Intent) {
        // remove something that the original FMS removes
        json.remove("androidx.contentpager.content.wakelockid")

        // get the message type
        when (val it = json.getString("message_type") ?: "gcm") {
            "gcm" -> { // 0
                if (shouldUploadMetrics(intent)) {
                    logNotificationReceived(intent, null)
                }

                onMessageReceived(Message(messageId, json))
            }
            "deleted_messages" -> { // 1
                onDeletedMessages()
            }
            "send_event" -> { // 2
                onMessageSent(messageId)
            }
            "send_error" -> { // 3
                onSendError(
                        messageId ?: json.getString("message_id"),
                        FirebaseSendException(json.getString("error"))
                )
            }
            else -> {
                Log.w(TAG, "Received message with unknown type: $it")
                return
            }
        }
    }

    data class Message(val messageId: String?, private val json: JsonObject) {
        val data = json.deepCopy()
        val from by lazy { s("test.from") ?: s("from") ?: "" }
        val to by lazy { s("google.to") }
        val messageType by lazy { s("message_type") }
        val collapseKey by lazy { s("collapse_key") }
        val sentTime by lazy { l("google.sent_time") }
        val ttl by lazy { i("google.ttl") }
        val originalPriority by lazy { getPriority(s("google.original_priority") ?: s("priority")) }
        val priority by lazy { getPriority(
                s("google.delivered_priority") ?: if (i("google.priority_reduced") == 1)
                    "normal"
                else s("google.priority")
        ) }
        val isNotificationMessage by lazy { isNotificationMessage(json) }
        val notificationTitle by lazy { s("gcm.notification.title") }
        val notificationText by lazy { s("gcm.notification.body") }

        init {
            data.also {
                val toRemove = mutableListOf<String>()
                it.keySet().forEach { key ->
                    if (key.startsWith("google.")
                            || key.startsWith("gcm.")
                            || key == "from"
                            || key == "message_type"
                            || key == "collapse_key")
                        toRemove += key
                }
                toRemove.forEach { key ->
                    it.remove(key)
                }
            }
        }

        private fun s(key: String): String? = json.getString(key)
        private fun l(key: String): Long = json.getLong(key) ?: 0L
        private fun i(key: String): Int = json.getInt(key) ?: 0
        private fun isNotificationMessage(json: JsonObject): Boolean {
            return json.getInt("gcm.n.e") == 1
                    || json.getInt("gcm.notification.e") == 1
                    || json.getString("gcm.n.icon") != null
                    || json.getString("gcm.notification.icon") != null
        }
        private fun getPriority(str: String?): Int {
            return when (str) {
                "high" -> 1
                "normal" -> 2
                else -> 0
            }
        }

        override fun toString(): String {
            return "Message(messageId=$messageId, from=$from, data=$data)"
        }
    }
}
