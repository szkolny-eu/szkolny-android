package pl.szczodrzynski.edziennik.widgets.timetable;

import android.content.Intent;
import android.widget.RemoteViewsService;

import pl.szczodrzynski.edziennik.widgets.timetable.WidgetTimetableListProvider;

public class WidgetTimetableService extends RemoteViewsService {
    /*
     * So pretty simple just defining the Adapter of the listview
     * here Adapter is ListProvider
     * */

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return (new WidgetTimetableListProvider(this.getApplicationContext(), intent));
    }

}