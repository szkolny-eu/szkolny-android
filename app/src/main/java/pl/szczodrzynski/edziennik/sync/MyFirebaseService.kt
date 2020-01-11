/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.sync

import android.util.Log
import pl.szczodrzynski.edziennik.App

class MyFirebaseService : FirebaseService() {
    companion object {
        private const val TAG = "MyFirebaseService"
    }

    private val app by lazy { applicationContext as App }

    override fun onNewToken(token: String?) {
        Log.d(TAG, "Got new token: $token")
        app.config.sync.tokenApp = token
    }

    override fun onMessageReceived(message: Message) {
        Log.d(TAG, "Message received from ${message.from}: $message")
    }
}
