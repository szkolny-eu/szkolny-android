package im.wangchao.mhttp.internal;

import im.wangchao.mhttp.BuildConfig;

/**
 * <p>Description  : Version.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/8/24.</p>
 * <p>Time         : 下午2:47.</p>
 */
public class Version {
    private Version(){
        throw new AssertionError();
    }

    public static String userAgent() {
        return moduleName().concat(BuildConfig.VERSION_NAME);
    }

    public static String moduleName() {
        return "mhttp";
    }
}
