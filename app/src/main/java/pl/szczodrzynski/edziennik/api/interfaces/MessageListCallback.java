package pl.szczodrzynski.edziennik.api.interfaces;

import java.util.List;

import pl.szczodrzynski.edziennik.datamodels.MessageFull;

public interface MessageListCallback {
    void onSuccess(List<MessageFull> messageList);
}
