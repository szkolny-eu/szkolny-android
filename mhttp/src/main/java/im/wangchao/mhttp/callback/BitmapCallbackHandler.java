package im.wangchao.mhttp.callback;

import android.graphics.Bitmap;

import im.wangchao.mhttp.AbsCallbackHandler;
import im.wangchao.mhttp.Accept;
import im.wangchao.mhttp.Response;

/**
 * <p>Description  : ImageResponseHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 15/10/18.</p>
 * <p>Time         : 下午2:49.</p>
 */
public abstract class BitmapCallbackHandler extends AbsCallbackHandler<Bitmap> {
    @Override public void onSuccess(Bitmap bitmap, Response response) {
    }

    @Override public void onFailure(Response response, Throwable throwable) {

    }

    @Override public String accept() {
        return Accept.ACCEPT_IMAGE;
    }

}
