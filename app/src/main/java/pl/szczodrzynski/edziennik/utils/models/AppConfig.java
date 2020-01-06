package pl.szczodrzynski.edziennik.utils.models;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.ui.widgets.WidgetConfig;

public class AppConfig {

    public List<Notification> notifications;

    public AppConfig(App _app) {
        notifications = new ArrayList<>();
    }

    public Map<Integer, WidgetConfig> widgetTimetableConfigs = new TreeMap<>();

    public boolean savePending = false;


    public String updateVersion = "";
    public String updateUrl = "";
    public String updateFilename = "";
    public boolean updateMandatory = false;
    public boolean updateDirect = false;

    public boolean webPushEnabled = false;


    public int mobidziennikOldMessages = -1;

    @NonNull
    public boolean dontShowAppManagerDialog = false;
}
