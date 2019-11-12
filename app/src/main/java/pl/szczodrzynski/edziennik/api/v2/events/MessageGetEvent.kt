/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-12.
 */

package pl.szczodrzynski.edziennik.api.v2.events

import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull

data class MessageGetEvent(val message: MessageFull)