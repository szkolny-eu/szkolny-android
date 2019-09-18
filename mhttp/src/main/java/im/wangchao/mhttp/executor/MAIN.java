package im.wangchao.mhttp.executor;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * <p>Description  : MainThread.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/9/2.</p>
 * <p>Time         : 下午4:45.</p>
 */
public final class MAIN implements Executor{
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override public void execute(@NonNull Runnable command) {
        handler.post(command);
    }
}
