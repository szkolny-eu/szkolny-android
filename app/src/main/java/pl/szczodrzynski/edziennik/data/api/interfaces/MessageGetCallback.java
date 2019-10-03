package pl.szczodrzynski.edziennik.data.api.interfaces;

import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull;

/**
 * Callback containing a {@link MessageFull} which already has its {@code body} and {@code recipients}.
 * {@code onSuccess} is always ran on the UI thread.
 */
public interface MessageGetCallback {
    void onSuccess(MessageFull message);
}
