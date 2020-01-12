/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.legacy.content.WakefulBroadcastReceiver
import com.google.gson.JsonObject

class FirebaseBroadcastReceiver : WakefulBroadcastReceiver() {
    companion object {
        private const val TAG = "FirebaseBroadcast"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras
        val json = JsonObject()
        extras?.keySet()?.forEach { key ->
            extras.get(key)?.let {
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
        Log.d(TAG, "Intent(action=${intent?.action}, extras=$json)")
    }
}