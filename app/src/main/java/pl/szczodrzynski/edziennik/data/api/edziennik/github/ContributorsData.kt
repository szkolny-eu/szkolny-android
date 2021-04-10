package pl.szczodrzynski.edziennik.data.api.edziennik.github

import android.util.Log
import okhttp3.*
import org.json.JSONArray
import pl.szczodrzynski.edziennik.App
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

class ContributorsData() {

    suspend fun getContributors(app: App): JSONArray {

        val request = Request.Builder()
            .url("https://api.github.com/repos/szkolny-eu/szkolny-android/contributors")
            .build()

        val call = app.http.newCall(request)

        return suspendCoroutine { cont ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful)
                        cont.resumeWith(Result.failure(IOException("Unexpected code $response")))

                    val jsonString = response.body()?.string()
                    val json = JSONArray(jsonString)
                    cont.resumeWith(Result.success(json))
                }
            })
        }
    }
}
