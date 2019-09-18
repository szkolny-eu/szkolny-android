package im.wangchao.mhttp;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>Description  : Method.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/3/8.</p>
 * <p>Time         : 下午4:08.</p>
 */
public interface Method {
    String POST = "POST";
    String GET  = "GET";
    String HEAD = "HEAD";
    String DELETE = "DELETE";
    String PUT = "PUT";
    String PATCH = "PATCH";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({Method.POST, Method.GET, Method.HEAD, Method.DELETE, Method.PUT, Method.PATCH})
    @interface MethodType{}
}
