package pl.szczodrzynski.edziennik.utils.models;

import java.util.List;

import static pl.szczodrzynski.edziennik.utils.Utils.contains;

public class Endpoint {
    public boolean defaultActive;

    public boolean onlyFullSync;

    public boolean enabled;

    public Endpoint(String name, boolean defaultActive, boolean onlyFullSync, List<String> changedEndpoints) {
        this.defaultActive = defaultActive;
        this.onlyFullSync = onlyFullSync;
        this.enabled = defaultActive;
        if (changedEndpoints == null)
            return;
        if (contains(changedEndpoints, name))
            this.enabled = !this.enabled;
        /*for (String changedEndpoint: changedEndpoints) {
            if (changedEndpoint.equals(name))
                this.enabled = !this.enabled;
        }*/
    }
}
