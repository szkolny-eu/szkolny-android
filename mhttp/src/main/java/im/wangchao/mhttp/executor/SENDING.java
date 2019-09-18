package im.wangchao.mhttp.executor;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * <p>Description  : SENDING.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/9/2.</p>
 * <p>Time         : 下午4:47.</p>
 */
public final class SENDING implements Executor {
    private final Handler handler;
    public SENDING(){
        if (Looper.myLooper() == null){
            throw new RuntimeException("The Looper of the current thread is null, please call Looper.prepare() on your thread.");
        }
        handler = new Handler(Looper.myLooper());
    }

    @Override public void execute(@NonNull Runnable command) {
        handler.post(command);
    }
}
