package im.wangchao.mhttp;

import java.util.concurrent.Executor;

import im.wangchao.mhttp.executor.BACKGROUND;
import im.wangchao.mhttp.executor.MAIN;
import im.wangchao.mhttp.executor.SENDING;

/**
 * <p>Description  : ThreadMode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/8/3.</p>
 * <p>Time         : 下午12:44.</p>
 */
public enum ThreadMode {
    /**
     * Callback will be called in the same thread, which is sending the request.
     */
    SENDING{
        @Override public Executor executor() {
            return new SENDING();
        }
    },
    /**
     * Callback will be called in Android's main thread (UI thread).
     */
    MAIN{
        @Override public Executor executor() {
            return new MAIN();
        }
    },

    /**
     * Callback will be called in a background thread. That is, work on the request thread(okhttp thread).
     */
    BACKGROUND{
        @Override public Executor executor() {
            return new BACKGROUND();
        }
    };

    public abstract Executor executor();
}
