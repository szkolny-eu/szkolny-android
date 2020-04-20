/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.response

data class ApiResponse<T> (

    val success: Boolean,

    val errors: List<Error>? = null,

    val data: T? = null
) {
    data class Error (val code: String, val reason: String)
}
