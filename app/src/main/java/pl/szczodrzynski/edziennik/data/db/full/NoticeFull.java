/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.full;

import pl.szczodrzynski.edziennik.data.db.entity.Notice;

public class NoticeFull extends Notice {
    public String teacherFullName = "";

    // metadata
    public boolean seen;
    public boolean notified;
    public long addedDate;
}
