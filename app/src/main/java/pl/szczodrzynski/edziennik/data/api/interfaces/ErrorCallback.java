package pl.szczodrzynski.edziennik.data.api.interfaces;

import android.content.Context;

import androidx.annotation.NonNull;

import pl.szczodrzynski.edziennik.data.api.AppError;

public interface ErrorCallback {
    void onError(Context activityContext, @NonNull AppError error);
}
