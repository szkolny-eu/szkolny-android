/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.core.firebase

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import kotlin.coroutines.CoroutineContext

class MyFirebaseService : FirebaseService(), CoroutineScope {
    companion object {
        private const val TAG = "MyFirebaseService"
    }

    private val app by lazy { applicationContext as App }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onNewToken(token: String?) {
        Log.d(TAG, "Got new token: $token")
        app.config.sync.tokenApp = token
    }

    override fun onMessageReceived(message: Message) {
        launch(Dispatchers.Default) {
            Log.d(TAG, "Message received from ${message.from}: $message")
            app.getSharedPreferences("firebase_service_log", Context.MODE_PRIVATE).edit().apply {
                putString(System.currentTimeMillis().toString(), message.toString())
                apply()
            }
            val profiles = app.db.profileDao().profilesForFirebaseNow
            when (message.from) {
                "640759989760" -> SzkolnyAppFirebase(app, profiles, message)
                "747285019373" -> SzkolnyMobidziennikFirebase(app, profiles, message)
                "513056078587" -> SzkolnyLibrusFirebase(app, profiles, message)
                "987828170337" -> SzkolnyVulcanFirebase(app, profiles, message)
            }
        }
    }
}
