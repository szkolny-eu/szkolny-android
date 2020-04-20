/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.full;

import pl.szczodrzynski.edziennik.data.db.entity.Attendance;

public class AttendanceFull extends Attendance {
    public String teacherFullName = "";

    public String subjectLongName = "";
    public String subjectShortName = "";

    // metadata
    public boolean seen;
    public boolean notified;
    public long addedDate;
}
