package im.wangchao.mhttp.callback;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import im.wangchao.mhttp.AbsCallbackHandler;
import im.wangchao.mhttp.Accept;
import im.wangchao.mhttp.Response;

/**
 * <p>Description  : JSONResponseHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 15/10/18.</p>
 * <p>Time         : 下午2:25.</p>
 */
public class JsonCallbackHandler extends AbsCallbackHandler<JsonObject> {

    @Override public void onSuccess(JsonObject data, Response response) {

    }

    @Override public void onFailure(Response response, Throwable throwable) {

    }

    @Override public JsonObject backgroundParser(Response response) throws Exception {
        try {
            byte[] body = response.raw().body().bytes();
            String bodyString = byteArrayToString(body);
            try {
                return new JsonParser().parse(bodyString).getAsJsonObject();
            }
            catch (Exception e) {
                e.printStackTrace();
                response.parserErrorBody = bodyString;
                return null;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override public String accept() {
        return Accept.ACCEPT_JSON;
    }
}
