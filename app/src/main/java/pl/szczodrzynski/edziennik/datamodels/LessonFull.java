package pl.szczodrzynski.edziennik.datamodels;

import androidx.room.ColumnInfo;
import android.content.Context;
import androidx.annotation.Nullable;

import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.utils.models.Date;

import static pl.szczodrzynski.edziennik.datamodels.LessonChange.TYPE_ADDED;
import static pl.szczodrzynski.edziennik.datamodels.LessonChange.TYPE_CANCELLED;
import static pl.szczodrzynski.edziennik.datamodels.LessonChange.TYPE_CHANGE;
import static pl.szczodrzynski.edziennik.utils.Utils.bs;

public class LessonFull extends Lesson {
    public String teacherFullName = "";

    public String subjectLongName = "";
    public String subjectShortName = "";

    public String teamName = "";

    @Nullable
    public Date lessonDate;

    @ColumnInfo(name = "lessonChangeId")
    public long changeId;
    @ColumnInfo(name = "lessonChangeType")
    public int changeType = -1;
    @ColumnInfo(name = "lessonChangeClassroomName")
    public String changeClassroomName = "";
    public String changeTeacherFullName = "";

    public String changeSubjectLongName = "";
    public String changeSubjectShortName = "";

    public String changeTeamName = "";

    public long changeTeacherId;
    public long changeSubjectId;
    public long changeTeamId;

    // metadata
    public boolean seen;
    public boolean notified;
    public long addedDate;

    public boolean lessonPassed;
    public boolean lessonCurrent;

    public boolean changedTeacherFullName() {
        return changeId != 0 && changeType != TYPE_CANCELLED && changeTeacherFullName != null && !changeTeacherFullName.equals(teacherFullName) && !changeTeacherFullName.equals("");
    }
    public boolean changedSubjectLongName() {
        return changeId != 0 && changeType != TYPE_CANCELLED && changeSubjectLongName != null && !changeSubjectLongName.equals(subjectLongName) && !changeSubjectLongName.equals("");
    }
    public boolean changedTeamName() {
        return changeId != 0 && changeType != TYPE_CANCELLED && changeTeamName != null && !changeTeamName.equals(teamName) && !changeTeamName.equals("");
    }
    public boolean changedClassroomName() {
        return changeId != 0 && changeType != TYPE_CANCELLED && changeClassroomName != null && !changeClassroomName.equals(classroomName) && !changeClassroomName.equals("");
    }

    public String getTeacherFullName() {
        return getTeacherFullName(false);
    }
    public String getTeacherFullName(boolean formatted) {
        if (!changedTeacherFullName())
            return bs(teacherFullName);
        else
            return (formatted?bs(teacherFullName)+" -> ":"") + bs(changeTeacherFullName);
    }

    public String getSubjectLongName() {
        return getSubjectLongName(false);
    }
    public String getSubjectLongName(boolean formatted) {
        if (!changedSubjectLongName())
            return bs(subjectLongName);
        else
            return (formatted?bs(subjectLongName)+" -> ":"") + bs(changeSubjectLongName);
    }

    public String getTeamName() {
        return getTeamName(false);
    }
    public String getTeamName(boolean formatted) {
        if (!changedTeamName())
            return bs(teamName);
        else
            return (formatted?bs(teamName)+" -> ":"") + bs(changeTeamName);
    }

    public String getClassroomName() {
        return getClassroomName(false);
    }
    public String getClassroomName(boolean formatted) {
        if (!changedClassroomName())
            return bs(classroomName);
        else
            return (formatted?bs(classroomName)+" -> ":"") + bs(changeClassroomName);
    }

    public String changeTypeStr(Context context) {
        if (changeType == TYPE_CANCELLED) {
            return context.getString(R.string.lesson_cancelled);
        }
        if (changeType == TYPE_CHANGE) {
            return context.getString(R.string.lesson_change);
        }
        if (changeType == TYPE_ADDED) {
            return context.getString(R.string.lesson_added);
        }
        return context.getString(R.string.lesson_timetable_change);
    }

    @Override
    public String toString() {
        return "LessonFull{" +
                "profileId=" + profileId +
                ", weekDay=" + weekDay +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", classroomName='" + classroomName + '\'' +
                ", teacherId=" + teacherId +
                ", subjectId=" + subjectId +
                ", teamId=" + teamId +
                ", teacherFullName='" + teacherFullName + '\'' +
                ", subjectLongName='" + subjectLongName + '\'' +
                ", subjectShortName='" + subjectShortName + '\'' +
                ", teamName='" + teamName + '\'' +
                ", lessonDate=" + lessonDate +
                ", changeId=" + changeId +
                ", changeType=" + changeType +
                ", changeClassroomName='" + changeClassroomName + '\'' +
                ", changeTeacherFullName='" + changeTeacherFullName + '\'' +
                ", changeSubjectLongName='" + changeSubjectLongName + '\'' +
                ", changeSubjectShortName='" + changeSubjectShortName + '\'' +
                ", changeTeamName='" + changeTeamName + '\'' +
                ", changeTeacherId=" + changeTeacherId +
                ", changeSubjectId=" + changeSubjectId +
                ", changeTeamId=" + changeTeamId +
                ", seen=" + seen +
                ", notified=" + notified +
                ", addedDate=" + addedDate +
                ", lessonPassed=" + lessonPassed +
                ", lessonCurrent=" + lessonCurrent +
                '}';
    }
}

