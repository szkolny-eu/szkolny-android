package im.wangchao.mhttp.internal.interceptor;

import java.io.IOException;

import im.wangchao.mhttp.internal.Singleton;
import im.wangchao.mhttp.internal.Version;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * <p>Description  : MBridgeInterceptors.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/8/24.</p>
 * <p>Time         : 下午4:32.</p>
 */
public final class MBridgeInterceptor implements Interceptor {
    private MBridgeInterceptor(){}

    public static Singleton<MBridgeInterceptor> instance = new Singleton<MBridgeInterceptor>() {
        @Override protected MBridgeInterceptor create() {
            return new MBridgeInterceptor();
        }
    };

    @Override public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder builder = request.newBuilder();

        if (request.header("User-Agent") == null) {
            builder.header("User-Agent", Version.userAgent());
        }

        return chain.proceed(builder.build());
    }
}
