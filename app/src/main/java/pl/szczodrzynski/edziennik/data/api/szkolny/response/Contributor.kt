package pl.szczodrzynski.edziennik.data.api.szkolny.request

data class Contributor(
    var contributors: List<ContributorItem>?,
    var translators: List<ContributorItem>?
    )

data class ContributorItem(
    var login: String?,
    var name: String?,
    var avatarUrl: String?,
    var profileUrl: String?,
    var contributions: Int?
)
