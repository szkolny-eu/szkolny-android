package pl.szczodrzynski.edziennik.datamodels;

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

