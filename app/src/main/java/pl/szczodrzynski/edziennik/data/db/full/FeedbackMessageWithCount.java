/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.full;

import pl.szczodrzynski.edziennik.data.db.entity.FeedbackMessage;

public class FeedbackMessageWithCount extends FeedbackMessage {
    public int messageCount = 0;
}
