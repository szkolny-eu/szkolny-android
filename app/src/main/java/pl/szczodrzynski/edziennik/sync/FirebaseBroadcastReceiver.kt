/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.sync

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.legacy.content.WakefulBroadcastReceiver
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App

class FirebaseBroadcastReceiver : WakefulBroadcastReceiver() {

    val TAG: String = FirebaseBroadcastReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {

        val json = JsonObject()

        val dataBundle = intent.extras
        if (dataBundle != null)
            for (key in dataBundle.keySet()) {
                dataBundle.get(key)?.let {
                    when (it) {
                        is String -> json.addProperty(key, it)
                        is Int -> json.addProperty(key, it)
                        is Long -> json.addProperty(key, it)
                        is Float -> json.addProperty(key, it)
                        is Boolean -> json.addProperty(key, it)
                        else -> json.addProperty(key, it.toString())
                    }
                }
            }

        Log.d(TAG, "Firebase got push from Librus Broadcast ${json}")

        val sharedPreferences = context.getSharedPreferences("pushtest_broadcast", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(
                System.currentTimeMillis().toString(),
                json.toString()
        ).apply()
    }
}