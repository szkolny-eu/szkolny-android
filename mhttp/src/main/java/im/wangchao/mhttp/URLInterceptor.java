package im.wangchao.mhttp;

import java.net.URL;

import okhttp3.HttpUrl;

/**
 * <p>Description  : URLInterceptor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/1/15.</p>
 * <p>Time         : 上午11:01.</p>
 */
public interface URLInterceptor {

    String interceptor(String origin);

    HttpUrl interceptor(HttpUrl origin);

    URL interceptor(URL origin);
}
