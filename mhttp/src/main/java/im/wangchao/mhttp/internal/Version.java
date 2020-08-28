package im.wangchao.mhttp.internal;

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
        return moduleName().concat("1.10.1");
    }

    public static String moduleName() {
        return "mhttp";
    }
}
