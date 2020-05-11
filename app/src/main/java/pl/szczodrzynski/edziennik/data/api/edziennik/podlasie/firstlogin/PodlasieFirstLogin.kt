package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.firstlogin

import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie

class PodlasieFirstLogin(val data: DataPodlasie, val onSuccess: () -> Unit) {
    companion object {
        const val TAG = "PodlasieFirstLogin"
    }

    init {
        // TODO
    }
}
