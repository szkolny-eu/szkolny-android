package im.wangchao.mhttp.callback;

import im.wangchao.mhttp.AbsCallbackHandler;
import im.wangchao.mhttp.Accept;
import im.wangchao.mhttp.Response;

/**
 * <p>Description  : BinaryResponseHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 15/10/18.</p>
 * <p>Time         : 下午2:49.</p>
 */
public class BinaryCallbackHandler extends AbsCallbackHandler<byte[]> {

    @Override public void onSuccess(byte[] data, Response response) {

    }

    @Override public void onFailure(Response response, Throwable throwable) {

    }

    @Override public byte[] backgroundParser(Response response) throws Exception {
        return response.raw().body().bytes();
    }

    @Override public String accept() {
        return Accept.ACCEPT_DATA;
    }
}
