package im.wangchao.mhttp.callback;

import im.wangchao.mhttp.AbsCallbackHandler;
import im.wangchao.mhttp.Accept;
import im.wangchao.mhttp.Response;

/**
 * <p>Description  : TextResponseHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 15/10/18.</p>
 * <p>Time         : 下午2:41.</p>
 */
public class TextCallbackHandler extends AbsCallbackHandler<String> {
    @Override public void onSuccess(String data, Response response) {
    }

    @Override public void onFailure(Response response, Throwable throwable) {
    }

    @Override public String backgroundParser(Response response) throws Exception {
        return byteArrayToString(response.raw().body().bytes());
    }

    @Override public String accept() {
        return Accept.ACCEPT_TEXT;
    }
}
