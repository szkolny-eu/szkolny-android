package pl.szczodrzynski.edziennik.receivers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

import pl.szczodrzynski.edziennik.sync.SyncJob;

public class JobsCreator implements JobCreator {
    @Override
    @Nullable
    public Job create(@NonNull String tag) {
        switch (tag) {
            case SyncJob.TAG:
                return new SyncJob();
            default:
                return null;
        }
    }
}
