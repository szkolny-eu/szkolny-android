/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-20.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.web

import com.google.gson.annotations.SerializedName

data class HomepageTile(
        @SerializedName("Nazwa")
        val name: String?,
        @SerializedName("Url")
        val url: String?,
        @SerializedName("Zawartosc")
        val children: List<HomepageTile>
)
