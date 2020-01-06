/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.full;

import pl.szczodrzynski.edziennik.data.db.entity.LuckyNumber;

public class LuckyNumberFull extends LuckyNumber {
    // metadata
    public boolean seen;
    public boolean notified;
    public long addedDate;
}

