package pl.szczodrzynski.edziennik.models;

import java.util.ArrayList;
import java.util.List;

public class ItemWidgetTimetableModel {
    public static final int EVENT_COLOR_HOMEWORK = -1;

    public String separatorProfileName = null;

    public int profileId;
    public Date lessonDate;
    public Time startTime;
    public Time endTime;
    public boolean lessonPassed;
    public boolean lessonCurrent;
    public String subjectName = "";
    public String classroomName = "";
    public boolean lessonChange = false;
    public String newSubjectName = null;
    public String newClassroomName = null;
    public boolean lessonCancelled = false;
    public long bellSyncDiffMillis = 0;
    public List<Integer> eventColors = new ArrayList<>();
    public boolean bigStyle = false;
    public boolean darkTheme = false;
}
