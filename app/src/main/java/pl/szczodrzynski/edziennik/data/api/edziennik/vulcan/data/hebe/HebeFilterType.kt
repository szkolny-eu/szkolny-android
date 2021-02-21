package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

enum class HebeFilterType(val endpoint: String) {
    BY_PUPIL("byPupil"),
    BY_PERSON("byPerson"),
    BY_PERIOD("byPeriod")
}
