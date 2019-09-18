package im.wangchao.mhttp.callback;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import im.wangchao.mhttp.AbsCallbackHandler;
import im.wangchao.mhttp.Accept;
import im.wangchao.mhttp.Response;

public class JsonArrayCallbackHandler extends AbsCallbackHandler<JsonArray> {

    @Override public void onSuccess(JsonArray data, Response response) {

    }

    @Override public void onFailure(Response response, Throwable throwable) {

    }

    @Override public JsonArray backgroundParser(Response response) throws Exception {
        try {
            byte[] body = response.raw().body().bytes();
            String bodyString = byteArrayToString(body);
            JsonArray object = new JsonParser().parse(bodyString).getAsJsonArray();
            return object;
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
