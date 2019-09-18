package im.wangchao.mhttp.executor;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * <p>Description  : BACKGROUND.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/9/2.</p>
 * <p>Time         : 下午4:50.</p>
 */
public final class BACKGROUND implements Executor {
    @Override public void execute(@NonNull Runnable command) {
        command.run();
    }
}
