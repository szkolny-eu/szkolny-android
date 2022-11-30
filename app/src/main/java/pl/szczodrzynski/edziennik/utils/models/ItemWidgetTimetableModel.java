package pl.szczodrzynski.edziennik.utils.models;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ItemWidgetTimetableModel {
    public static final int EVENT_COLOR_HOMEWORK = -1;

    public String separatorProfileName = null;

    public int profileId;
    public long lessonId;
    public Date lessonDate;
    @NonNull
    public Time startTime;
    @NonNull
    public Time endTime;
    public boolean lessonPassed;
    public boolean lessonCurrent;
    public CharSequence subjectName = "";
    public String classroomName = "";
    public boolean lessonChange = false;
    public boolean lessonChangeNoClassroom = false;
    public String newSubjectName = null;
    public String newClassroomName = null;
    public boolean lessonCancelled = false;
    public long bellSyncDiffMillis = 0;
    public List<Integer> eventColors = new ArrayList<>();
    public boolean bigStyle = false;
    public boolean darkTheme = false;

    public boolean isNoTimetableItem = false;
    public boolean isNoLessonsItem = false;
    public boolean isNotPublicItem = false;
}
