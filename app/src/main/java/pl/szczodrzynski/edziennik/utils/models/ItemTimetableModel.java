package pl.szczodrzynski.edziennik.utils.models;

import java.util.List;

import pl.szczodrzynski.edziennik.data.db.modules.events.Event;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;

public class ItemTimetableModel {
    public LessonFull lesson;
    public List<Event> events;
    public Date lessonDate;

    public ItemTimetableModel(LessonFull lesson, List<Event> events, Date lessonDate) {
        this.lesson = lesson;
        this.events = events;
        this.lessonDate = lessonDate;
    }
}
