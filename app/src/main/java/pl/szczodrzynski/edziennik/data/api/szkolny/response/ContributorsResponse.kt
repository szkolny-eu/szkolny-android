package pl.szczodrzynski.edziennik.data.api.szkolny.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class ContributorsResponse(
    val contributors: List<Item>,
    val translators: List<Item>
) {

    @Parcelize
    data class Item(
        val login: String,
        val name: String?,
        val avatarUrl: String,
        val profileUrl: String,
        val itemUrl: String,
        val contributions: Int?
    ) : Parcelable
}
