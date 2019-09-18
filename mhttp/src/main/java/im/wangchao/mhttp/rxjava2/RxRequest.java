package im.wangchao.mhttp.rxjava2;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executor;

import im.wangchao.mhttp.AbsCallbackHandler;
import im.wangchao.mhttp.Converter;
import im.wangchao.mhttp.Method;
import im.wangchao.mhttp.Request;
import im.wangchao.mhttp.RequestParams;
import im.wangchao.mhttp.Response;
import im.wangchao.mhttp.ThreadMode;
import io.reactivex.Observable;
import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;

/**
 * <p>Description  : RxRequest.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/3/26.</p>
 * <p>Time         : 下午5:40.</p>
 */
public class RxRequest<T> {
    public static <T> RxRequest.Builder<T> builder(){
        return new RxRequest.Builder<>();
    }

    private Request request;
    private AbsCallbackHandler<T> callback;

    private RxRequest(Builder<T> builder){
        request = builder.request;
        callback = builder.callback;
    }

    public Request request(){
        return request;
    }

    public AbsCallbackHandler<T> callback(){
        return callback;
    }

    public RxRequest.Builder<T> newBuilder() {
        return new Builder<>(this);
    }

    public Observable<T> execute(Converter<Response, T> converter){
        return new ResponseExecuteObservable<>(this, converter);
    }

    public Observable<T> enqueue(){
        return new ResponseEnqueueObservable<>(this);
    }

    public static final class Builder<T> {

        Request request;
        Request.Builder requestBuilder;
        AbsCallbackHandler<T> callback;

        public Builder() {
            requestBuilder = new Request.Builder();
            callback = new AbsCallbackHandler<T>() {
                @Override public void onSuccess(T data, Response response) {

                }

                @Override public void onFailure(Response response, Throwable throwable) {

                }
            };
        }

        private Builder(RxRequest<T> request) {
            requestBuilder = request.request.newBuilder();
            callback = request.callback;
        }

        public Builder<T> url(HttpUrl url) {
            requestBuilder.url(url);
            return this;
        }

        public Builder<T> url(String url) {
            requestBuilder.url(url);
            return this;
        }

        public Builder<T> url(URL url) {
            requestBuilder.url(url);
            return this;
        }

        public Builder<T> header(String name, String value) {
            requestBuilder.header(name, value);
            return this;
        }

        public Builder<T> addHeader(String name, String value) {
            requestBuilder.header(name, value);
            return this;
        }

        public Builder<T> removeHeader(String name) {
            requestBuilder.removeHeader(name);
            return this;
        }

        public Builder<T> headers(Headers headers) {
            requestBuilder.headers(headers);
            return this;
        }

        public Builder<T> cacheControl(CacheControl cacheControl) {
            requestBuilder.cacheControl(cacheControl);
            return this;
        }

        public Builder<T> get() {
            return method(Method.GET);
        }

        public Builder<T> head() {
            return method(Method.HEAD);
        }

        public Builder<T> post() {
            return method(Method.POST);
        }

        public Builder<T> delete() {
            return method(Method.DELETE);
        }

        public Builder<T> put() {
            return method(Method.PUT);
        }

        public Builder<T> patch() {
            return method(Method.PATCH);
        }

        /**
         * Simple to add request parameter
         */
        public Builder<T> addParameter(String key, Object value){
            requestBuilder.addParameter(key, value);
            return this;
        }

        /**
         * Simple to add request parameter
         */
        public Builder<T> addParameter(String key, InputStream stream, String name){
            requestBuilder.addParameter(key, stream, name);
            return this;
        }

        /**
         * Simple to add request parameter
         */
        public Builder<T> addParameter(String key, InputStream stream, String name, String contentType){
            requestBuilder.addParameter(key, stream, name, contentType);
            return this;
        }

        /**
         * Simple to add request parameter
         */
        public Builder<T> addParameter(String key, File file, String contentType) {
            requestBuilder.addParameter(key, file, contentType);
            return this;
        }

        public Builder<T> requestParams(RequestParams params) {
            requestBuilder.requestParams(params);
            return this;
        }

        public Builder<T> method(@NonNull String method) {
            requestBuilder.method(method);
            return this;
        }

        public Builder<T> tag(Object tag) {
            requestBuilder.tag(tag);
            return this;
        }

        public Builder<T> callback(@NonNull AbsCallbackHandler<T> callback){
            this.callback = callback;
            return this;
        }

        public Builder<T> callbackExecutor(Executor executor){
            requestBuilder.callbackExecutor(executor);
            return this;
        }

        public Builder<T> callbackThreadMode(ThreadMode threadMode){
            requestBuilder.callbackThreadMode(threadMode);
            return this;
        }

        public Builder<T> userAgent(String ua){
            requestBuilder.userAgent(ua);
            return this;
        }

        public Builder<T> contentType(@NonNull String contentType){
            requestBuilder.contentType(contentType);
            return this;
        }

        public Builder<T> contentType(@NonNull MediaType mediaType){
            requestBuilder.contentType(mediaType);
            return this;
        }

        public RxRequest<T> build() {
            request = requestBuilder.build();

            return new RxRequest<>(this);
        }
    }
}
