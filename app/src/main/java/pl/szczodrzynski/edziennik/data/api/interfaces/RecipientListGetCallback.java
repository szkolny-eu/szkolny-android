package pl.szczodrzynski.edziennik.data.api.interfaces;

import java.util.List;

import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher;

public interface RecipientListGetCallback {
    void onSuccess(List<Teacher> teacherList);
}
