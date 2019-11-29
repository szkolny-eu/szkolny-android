package pl.szczodrzynski.edziennik.utils.models;

import androidx.annotation.Nullable;

import java.util.List;

import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeFull;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject;

public class ItemGradesSubjectModel {
    public Profile profile;
    public Subject subject;
    public List<GradeFull> grades1;
    public List<GradeFull> grades2;

    public int semester1Unread = 0;
    public int semester2Unread = 0;

    public float semester1Average = -1;
    public GradeFull semester1Proposed = null;
    public GradeFull semester1Final = null;

    public float semester2Average = -1;
    public GradeFull semester2Proposed = null;
    public GradeFull semester2Final = null;

    public float yearAverage = -1;
    public GradeFull yearProposed = null;
    public GradeFull yearFinal = null;

    public float gradeSumSemester1 = 0;
    public float gradeCountSemester1 = 0;

    public float gradeSumSemester2 = 0;
    public float gradeCountSemester2 = 0;

    public float gradeSumOverall = 0;
    public float gradeCountOverall = 0;

    public boolean isNormalSubject = false;
    public boolean isPointSubject = false;
    public boolean isBehaviourSubject = false;
    public boolean isDescriptiveSubject = false;

    public boolean expandView = false;

    public static @Nullable
    ItemGradesSubjectModel searchModelBySubjectId(List<ItemGradesSubjectModel> subjectList, long id) {
        for (ItemGradesSubjectModel subjectModel : subjectList) {
            if (subjectModel.subject != null && subjectModel.subject.id == id) {
                return subjectModel;
            }
        }
        return null;
    }

    public ItemGradesSubjectModel(Profile profile, Subject subject, List<GradeFull> grades1, List<GradeFull> grades2) {
        this.profile = profile;
        this.subject = subject;
        this.grades1 = grades1;
        this.grades2 = grades2;
    }

    @Override
    public String toString() {
        return "ItemGradesSubjectModel{" +
                "profile=" + profile +
                ", subject=" + subject +
                ", grades1=" + grades1 +
                ", grades2=" + grades2 +
                ", semester1Unread=" + semester1Unread +
                ", semester2Unread=" + semester2Unread +
                ", semester1Average=" + semester1Average +
                ", semester1Proposed=" + semester1Proposed +
                ", semester1Final=" + semester1Final +
                ", semester2Average=" + semester2Average +
                ", semester2Proposed=" + semester2Proposed +
                ", semester2Final=" + semester2Final +
                ", yearAverage=" + yearAverage +
                ", yearProposed=" + yearProposed +
                ", yearFinal=" + yearFinal +
                ", gradeSumSemester1=" + gradeSumSemester1 +
                ", gradeCountSemester1=" + gradeCountSemester1 +
                ", gradeSumSemester2=" + gradeSumSemester2 +
                ", gradeCountSemester2=" + gradeCountSemester2 +
                ", gradeSumOverall=" + gradeSumOverall +
                ", gradeCountOverall=" + gradeCountOverall +
                ", expandView=" + expandView +
                '}';
    }
}
