/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2.librus

import android.util.Log
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.CODE_INTERNAL_LIBRUS_ACCOUNT_410
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_NOT_NEEDED
import pl.szczodrzynski.edziennik.api.v2.endpoints
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librusLoginMethods
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.api.v2.models.Endpoint
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile

class Librus(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        const val TAG = "Librus"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataLibrus

    init {
        data = DataLibrus(app, profile, loginStore).apply {
            callback = wrapCallback(this@Librus.callback)
        }
        data.satisfyLoginMethods()
    }

    override fun sync(featureIds: List<Int>) {
        val possibleLoginMethods = data.loginMethods.toMutableList()

        for (loginMethod in librusLoginMethods) {
            if (loginMethod.isPossible(profile, loginStore))
                possibleLoginMethods += loginMethod.loginMethodId
        }

        //var highestLoginMethod = 0
        var endpointList = mutableListOf<Endpoint>()
        val requiredLoginMethods = mutableListOf<Int>()

        var targetEndpointIds = mutableListOf<Int>()
        var targetLoginMethodIds = mutableListOf<Int>()

        // get all endpoints for every feature, only if possible to login
        for (featureId in featureIds) {
            /*endpoints.filter { it.featureId == featureId }.forEach { endpoint ->
                if (possibleLoginMethods.containsAll(endpoint.requiredLoginMethods)) {
                    endpointList.add(endpoint)
                    //highestLoginMethod = max(highestLoginMethod, endpoint.requiredLoginMethods.max() ?: 0)
                }
            }*/
            endpoints.filter {
                        it.featureId == featureId && possibleLoginMethods.containsAll(it.requiredLoginMethods)
                    }
                    .let {
                        endpointList.addAll(it)
                    }
        }

        endpointList = endpointList
                // sort the endpoint list by feature ID and priority
                .sortedWith(compareBy(Endpoint::featureId, Endpoint::priority))
                // select only the most important endpoint for each feature
                .distinctBy { it.featureId }
                .toMutableList()
                // add all endpoint IDs and required login methods
                .onEach { endpoint ->
                    targetEndpointIds.addAll(endpoint.endpointIds)
                    requiredLoginMethods.addAll(endpoint.requiredLoginMethods)
                }

        // check every login method for any dependencies
        for (loginMethodId in requiredLoginMethods) {
            var requiredLoginMethod: Int? = loginMethodId
            while (requiredLoginMethod != LOGIN_METHOD_NOT_NEEDED) {
                librusLoginMethods.singleOrNull { it.loginMethodId == requiredLoginMethod }?.let { loginMethod ->
                    if (requiredLoginMethod != null)
                        targetLoginMethodIds.add(requiredLoginMethod!!)
                    requiredLoginMethod = loginMethod.requiredLoginMethod(data.profile, data.loginStore)
                }
            }
        }

        // sort and distinct every login method and endpoint
        targetLoginMethodIds = targetLoginMethodIds.toHashSet().toMutableList()
        targetLoginMethodIds.sort()

        targetEndpointIds = targetEndpointIds.toHashSet().toMutableList()
        targetLoginMethodIds.sort()



        //Log.d(TAG, endpointList.toString())
        Log.d(TAG, "LoginMethod IDs: $targetLoginMethodIds")
        Log.d(TAG, "Endpoint IDs: $targetEndpointIds")

        /*

        INPUT: [
            FEATURE_GRADES,
            FEATURE_STUDENT_INFO,
            FEATURE_STUDENT_NUMBER
        ]

        OUTPUT: [
            Endpoint(loginType=2,
                featureId=FEATURE_GRADES, endpointIds=[
                    ENDPOINT_LIBRUS_API_NORMAL_GC,
                    ENDPOINT_LIBRUS_API_NORMAL_GRADES,
                    ENDPOINT_LIBRUS_SYNERGIA_GRADES
                ], requiredLoginMethods=[
                    LOGIN_METHOD_LIBRUS_API,
                    LOGIN_METHOD_LIBRUS_SYNERGIA
            ]),
            Endpoint(loginType=2,
                featureId=FEATURE_STUDENT_INFO, endpointIds=[
                    ENDPOINT_LIBRUS_API_ME
                ], requiredLoginMethods=[
                    LOGIN_METHOD_LIBRUS_API
            ]),
            Endpoint(loginType=2,
                featureId=FEATURE_STUDENT_NUMBER, endpointIds=[
                    ENDPOINT_LIBRUS_SYNERGIA_INFO
                ], requiredLoginMethods=[
                    LOGIN_METHOD_LIBRUS_SYNERGIA
            ])
        ]

         */
    }

    override fun getMessage(messageId: Int) {

    }

    private fun wrapCallback(callback: EdziennikCallback): EdziennikCallback {
        return object : EdziennikCallback {
            override fun onCompleted() {
                callback.onCompleted()
            }

            override fun onProgress(step: Int) {
                callback.onProgress(step)
            }

            override fun onStartProgress(stringRes: Int) {
                callback.onStartProgress(stringRes)
            }

            override fun onError(apiError: ApiError) {
                when (apiError.errorCode) {
                    in internalErrorList -> {
                        // finish immediately if the same error occurs twice during the same sync
                        callback.onError(apiError)
                    }
                    CODE_INTERNAL_LIBRUS_ACCOUNT_410 -> {
                        internalErrorList.add(apiError.errorCode)
                        loginStore.removeLoginData("refreshToken") // force a clean login
                        //loginLibrus()
                    }
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}