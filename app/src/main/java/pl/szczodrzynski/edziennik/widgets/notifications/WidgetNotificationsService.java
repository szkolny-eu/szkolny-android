package pl.szczodrzynski.edziennik.widgets.notifications;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class WidgetNotificationsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return (new WidgetNotificationsListProvider(this.getApplicationContext(), intent));
    }
}
