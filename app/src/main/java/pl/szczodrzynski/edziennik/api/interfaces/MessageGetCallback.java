package pl.szczodrzynski.edziennik.api.interfaces;

import pl.szczodrzynski.edziennik.datamodels.Message;
import pl.szczodrzynski.edziennik.datamodels.MessageFull;

/**
 * Callback containing a {@link MessageFull} which already has its {@code body} and {@code recipients}.
 * {@code onSuccess} is always ran on the UI thread.
 */
public interface MessageGetCallback {
    void onSuccess(MessageFull message);
}
