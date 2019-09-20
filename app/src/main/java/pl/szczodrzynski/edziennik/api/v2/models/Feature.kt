package pl.szczodrzynski.edziennik.api.v2.models

import pl.szczodrzynski.edziennik.api.v2.endpoint


data class Feature(val featureId: Int, val loginOptions: Map<Int, List<Int>>) {

    init {

    }

}