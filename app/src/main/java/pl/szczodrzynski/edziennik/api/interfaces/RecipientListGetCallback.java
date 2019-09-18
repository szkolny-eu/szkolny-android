package pl.szczodrzynski.edziennik.api.interfaces;

import java.util.List;

import pl.szczodrzynski.edziennik.datamodels.Teacher;

public interface RecipientListGetCallback {
    void onSuccess(List<Teacher> teacherList);
}
