/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.full;

import pl.szczodrzynski.edziennik.data.db.entity.Grade;

public class GradeFull extends Grade {
    //public String category = "";
    //public int color;

    public String subjectLongName = "";
    public String subjectShortName = "";

    public String teacherFullName = "";

    // metadata
    public boolean seen;
    public boolean notified;
    public long addedDate;
}

