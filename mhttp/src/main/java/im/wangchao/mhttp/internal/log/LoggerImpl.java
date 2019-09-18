package im.wangchao.mhttp.internal.log;

import android.util.Log;

import im.wangchao.mhttp.internal.Singleton;
import im.wangchao.mhttp.internal.Version;
import im.wangchao.mhttp.internal.interceptor.HttpLoggingInterceptor;

/**
 * <p>Description  : Logger.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/3/20.</p>
 * <p>Time         : 下午5:36.</p>
 */
public class LoggerImpl implements HttpLoggingInterceptor.Logger{
    private static final String TAG = Version.moduleName();
    private HttpLoggingInterceptor.Level mLevel;

    public static Singleton<LoggerImpl> instance = new Singleton<LoggerImpl>() {
        @Override protected LoggerImpl create() {
            return new LoggerImpl();
        }
    };

    public void setLevel(HttpLoggingInterceptor.Level level){
        mLevel = level;
    }

    @Override public void log(String message) {
        if (mLevel == HttpLoggingInterceptor.Level.NONE){
            return;
        }
        Log.e(TAG, message);
    }
}
