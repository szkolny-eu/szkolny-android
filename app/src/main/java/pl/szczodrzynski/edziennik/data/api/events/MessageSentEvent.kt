/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-27.
 */

package pl.szczodrzynski.edziennik.data.api.events

import pl.szczodrzynski.edziennik.data.db.modules.messages.Message

data class MessageSentEvent(val profileId: Int, val message: Message?, val sentDate: Long?)
