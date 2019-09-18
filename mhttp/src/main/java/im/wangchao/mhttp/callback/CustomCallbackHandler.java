package im.wangchao.mhttp.callback;

import im.wangchao.mhttp.Response;
import im.wangchao.mhttp.internal.exception.ParserException;

/**
 * <p>Description  : GSONResponseHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/3/20.</p>
 * <p>Time         : 上午9:06.</p>
 */
public abstract class CustomCallbackHandler<T> extends TextCallbackHandler {

    @Override final public void onSuccess(String data, Response response) {
        if (data != null) {
            onSuccess(parser(data));
        }
        else {
            onFailure(response, new ParserException());
        }
    }

    @Override public void onFailure(Response response, Throwable throwable) {

    }

    /** parser Json to T */
    protected abstract T parser(String data);

    public void onSuccess(T t){

    }

}
