package pl.szczodrzynski.edziennik.data.api.interfaces;

import im.wangchao.mhttp.Request;

/**
 * Callback containing a {@link Request.Builder} which has correct headers and body to download a corresponding message attachment when ran.
 * {@code onSuccess} has to be ran on the UI thread.
 */
public interface AttachmentGetCallback {
    void onSuccess(Request.Builder builder);
}
