/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2.interfaces

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile

abstract class ILoginMethod(
        val app: App,
        val profile: Profile?,
        val loginStore: LoginStore,
        val callback: ProgressCallback,
        val onSuccess: () -> Unit
) {

}