/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-20.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

enum class HebeFilterType(val endpoint: String) {
    BY_MESSAGEBOX("byBox"),
    BY_PUPIL("byPupil"),
    BY_PERSON("byPerson"),
    BY_PERIOD("byPeriod")
}
