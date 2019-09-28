package pl.szczodrzynski.edziennik.data.api.interfaces;

import java.util.List;

import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull;

public interface MessageListCallback {
    void onSuccess(List<MessageFull> messageList);
}
