package pl.szczodrzynski.edziennik.data.api.edziennik.github

import com.google.gson.GsonBuilder
import okhttp3.*
import pl.szczodrzynski.edziennik.data.api.edziennik.github.Contributor
import pl.szczodrzynski.edziennik.App
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

object ContributorsData {

    suspend fun getContributors(app: App): List<Contributor> {

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

                    val gson = GsonBuilder().create()

                    val body = response.body()?.string()
                    val json = gson.fromJson(body, Array<Contributor>::class.java).toList()
                    cont.resumeWith(Result.success(json))
                }
            })
        }
    }
}
