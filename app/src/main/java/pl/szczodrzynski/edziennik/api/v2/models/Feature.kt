package pl.szczodrzynski.edziennik.api.v2.models


data class Feature(val featureId: Int, val loginOptions: Map<Int, List<Int>>) {

    init {

    }

}