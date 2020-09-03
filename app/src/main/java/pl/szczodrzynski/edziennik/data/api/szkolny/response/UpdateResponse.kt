/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-18.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.response

data class Update(
        val versionCode: Int,
        val versionName: String,
        val releaseDate: String,
        val releaseNotes: String?,
        val releaseType: String,
        val isOnGooglePlay: Boolean,
        val downloadUrl: String?,
        val updateMandatory: Boolean
)
