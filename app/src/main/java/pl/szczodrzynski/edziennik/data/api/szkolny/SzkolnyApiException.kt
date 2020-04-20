/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-16.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny

import pl.szczodrzynski.edziennik.data.api.szkolny.response.ApiResponse

class SzkolnyApiException(val error: ApiResponse.Error?) : Exception(if (error == null) "Error body does not contain a valid Error." else "${error.code}: ${error.reason}")
