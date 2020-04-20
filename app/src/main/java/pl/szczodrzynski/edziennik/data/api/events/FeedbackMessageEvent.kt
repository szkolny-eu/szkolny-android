/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-21.
 */

package pl.szczodrzynski.edziennik.data.api.events

import pl.szczodrzynski.edziennik.data.db.entity.FeedbackMessage

data class FeedbackMessageEvent(val message: FeedbackMessage)
