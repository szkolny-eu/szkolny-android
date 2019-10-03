/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2.librus

import android.content.Intent
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.ApiService
import pl.szczodrzynski.edziennik.api.v2.LOGIN_MODE_LIBRUS_EMAIL
import pl.szczodrzynski.edziennik.api.v2.LOGIN_TYPE_LIBRUS
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

class LibrusTest(val app: App) {
    companion object {
        private const val TAG = "LibrusTest"
    }

    val profile = Profile(1, "Profil", "xd", 1).apply {
        //putStudentData("accountLogin", "1234567")

        //putStudentData("accountCode", LIBRUS_JST_DEMO_CODE)
        //putStudentData("accountPin", LIBRUS_JST_DEMO_PIN)

        putStudentData("accountLogin", "1234567")

        //putStudentData("accountToken", "token")
        //putStudentData("accountTokenTime", 1569458277)
    }
    val loginStore = LoginStore(1, LOGIN_TYPE_LIBRUS, JsonObject().apply {
        addProperty("email", "test@example.com")
        addProperty("password", "zaq1@WSX")

        //addProperty("accessToken", "token")
        //addProperty("refreshToken", "refresh")
        //addProperty("tokenExpiryTime", 1569523077)
    }).also {
        it.mode = LOGIN_MODE_LIBRUS_EMAIL
    }

    fun go() {

        /*Librus(app, profile, loginStore, object : EdziennikCallback {
            override fun onCompleted() {}
            override fun onError(apiError: ApiError) {}
            override fun onProgress(step: Int) {}
            override fun onStartProgress(stringRes: Int) {}
        }).sync(listOf(FEATURE_GRADES, FEATURE_STUDENT_INFO, FEATURE_STUDENT_NUMBER))*/

        app.startService(Intent(app, ApiService::class.java))

        if (false) {

        }

        /*val data = DataLibrus(app, profile, loginStore).apply {
            callback = object : ProgressCallback {
                override fun onProgress(progressStep: Int) {

                }

                override fun onActionStarted(stringResId: Int) {
                    d(TAG, app.getString(stringResId))
                }

                override fun onError(activityContext: Context?, error: AppError) {
                    error.changeIfCodeOther()
                    d(TAG, "Error "+error.getDetails(app))
                }
            }
        }*/

        /*LoginLibrus(data, LOGIN_METHOD_LIBRUS_MESSAGES) {
            d(TAG, "Login succeeded.")
            d(TAG, "Profile data: ${data.profile?.studentData?.toString()}")
            d(TAG, "LoginStore data: ${data.loginStore.data}")
        }*/
    }
}
