package im.wangchao.mhttp;


import android.util.Log;

/**
 * <p>Description  : BindApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 15/10/19.</p>
 * <p>Time         : 上午8:23.</p>
 */
final class BindApi {
    /** HttpProcessor.SUFFIX */
    private static final String SUFFIX = "_HttpBinder";

    @SuppressWarnings("unchecked") public static <T> T bind(Class<T> type) {
        String name = type.getName() + SUFFIX;
        T obj = null;
        try {
            obj = (T)Class.forName(name).newInstance();
        } catch (Exception e) {
            Log.e(BindApi.class.getSimpleName(), e.getMessage(), e);
        }
        return obj;
    }


}
