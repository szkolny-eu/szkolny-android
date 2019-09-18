package im.wangchao.mhttp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import im.wangchao.mhttp.internal.exception.ParserException;
import im.wangchao.mhttp.internal.exception.ResponseFailException;
import okhttp3.Call;
import okhttp3.internal.Util;

import static im.wangchao.mhttp.Response.IO_EXCEPTION_CODE;

/**
 * <p>Description  : AbsResponseHandler.
 *                   Callback lifecycle as follow:
 *                                              onStart()
 *                         -------------------------------------------------------
 *                              |
 *                              |
 *                         is canceled --- Y --- onCancel()
 *                              |
 *                              N
 *                              |
 *                          onFinish()
 *                              |
 *                              |
 *                        is successful --- N --- onFailure() ------------------
 *                              |                                                 |
 *                              Y                                                 |
 *                              |                                                 |
 *                        backgroundParser() --is download-- onProgress()         |
 *                              |                                     |           |
 *                              |                                     |           |
 *                          onSuccess()                           onSuccess()     |
 *                              |                                     |           |
 *                              |                                     |           |
 *                        ---------------------------------------------------------
 *                                             onFinally()
 *                          </p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 15/8/17.</p>
 * <p>Time         : 下午5:56.</p>
 */
public abstract class AbsCallbackHandler<Parser_Type> implements Callback, Converter<Response, Parser_Type>{
    private final static ExecutorService DEFAULT_EXECUTOR_SERVICE = Executors.newCachedThreadPool(Util.threadFactory("OkHttp", false));

    public final static String  DEFAULT_CHARSET     = "UTF-8";

    private Request request;
    private String responseCharset = DEFAULT_CHARSET;
    private boolean isFinished;
    private boolean isCanceled;

    private Executor mExecutor;

    /** Working thread depends on {@link #mExecutor}, default UI. */
    public abstract void onSuccess(Parser_Type data, Response response);
    /** Working thread depends on {@link #mExecutor}, default UI. */
    public abstract void onFailure(Response response, Throwable throwable);
    /** Work on the request thread, that is okhttp thread. */
    @Deprecated
    public Parser_Type backgroundParser(Response response) throws Exception{
        return null;
    }
    /** Work on the request thread, that is okhttp thread. */
    @Override public Parser_Type apply(Response response) throws Exception {
        return backgroundParser(response);
    }

    /** Working thread depends on {@link #mExecutor}, default UI. */
    public void onStart(){}
    /** Working thread depends on {@link #mExecutor}, default UI. */
    public void onCancel(){}
    /** Working thread depends on {@link #mExecutor}, default UI. */
    public void onProgress(long bytesWritten, long bytesTotal){}
    /** Working thread depends on {@link #mExecutor}, default UI. */
    public void onUploadProgress(int bytesWritten, int bytesTotal){}
    /** Working thread depends on {@link #mExecutor}, default UI. */
    public void onFinish(){}
    /** Working thread depends on {@link #mExecutor}, default UI. */
    public void onFinally(Response response){}


    @Override final public void onFailure(@NonNull Call call, @NonNull IOException e) {
        if (call.isCanceled()){
            sendCancelEvent();
            return;
        }
        sendFinishEvent();

        final Request req = request;
        Response response = Response.error(req,
                IO_EXCEPTION_CODE,
                e.getMessage());

        sendFailureEvent(response, e);
        sendFinallyEvent(response);
    }

    @Override final public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
        if (call.isCanceled()){
            response.close();
            sendCancelEvent();
            return;
        }
        sendFinishEvent();

        final Request req = request;
        Response okResponse;
        if (response.isSuccessful() || response.isRedirect()
                || req.allowedErrorCodes().contains(response.code())) {
            try {
                okResponse = Response.newResponse(req, response);
                Parser_Type data = apply(okResponse);
                sendSuccessEvent(data, okResponse);
            } catch (Exception e) {
                sendFailureEvent(okResponse = Response.newResponse(req, response), e);
            }
        } else {
            sendFailureEvent(okResponse = Response.newResponse(req, response), new ResponseFailException());
        }
        // todo response.close()
        sendFinallyEvent(okResponse);
    }

    public AbsCallbackHandler(){}

    @Override public void initialize(Request request){
        isFinished = false;
        isCanceled = false;
        this.request = request;
        this.mExecutor = request.callbackExecutor();
        if (this.mExecutor == null){
            this.mExecutor = request.callbackThreadMode().executor();
        }
    }

    public final boolean isFinished(){
        return isFinished;
    }

    /**
     * Sets the charset for the response string. If not set, the default is UTF-8.
     */
    public final void setCharset(@NonNull final String charset) {
        this.responseCharset = charset;
    }

    /**
     * subclass can override this method to change charset.
     */
    public String charset() {
        return TextUtils.isEmpty(responseCharset) ? DEFAULT_CHARSET : responseCharset;
    }

    /**
     * @return request accept
     */
    @Override public String accept(){
        return Accept.EMPTY;
    }

    protected final void print(String message){
        Log.d(AbsCallbackHandler.class.getSimpleName(), message);
    }

    @Nullable protected final String byteArrayToString(byte[] bytes){
        try {
            return bytes == null ? null : new String(bytes, charset());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    protected final Request getRequest(){
        return this.request;
    }

    public final void sendUploadProgressEvent(final int bytesWritten, final int bytesTotal) {
        execute(()->{
            try {
                onUploadProgress(bytesWritten, bytesTotal);
            } catch (Throwable t) {
                //Silent
            }
        });
    }

    public final void sendProgressEvent(final long bytesWritten, final long bytesTotal) {
        execute(()->{
            try {
                onProgress(bytesWritten, bytesTotal);
            } catch (Throwable t) {
                //Silent
            }
        });
    }

    /*package*/ final void sendSuccessEvent(final Parser_Type data, final Response response) {
        execute(() -> onSuccess(data, response));
    }

    /*package*/ final void sendFailureEvent(final Response response, @Nullable final Throwable throwable) {
        execute(() -> onFailure(response, throwable));
    }

    /*package*/ final void sendStartEvent() {
        if (request.callbackThreadMode() == ThreadMode.BACKGROUND){
            DEFAULT_EXECUTOR_SERVICE.execute(this::onStart);
        } else {
            execute(this::onStart);
        }
    }

    /*package*/ final void sendFinishEvent() {
        execute(() -> {
            AbsCallbackHandler.this.isFinished = true;
            onFinish();
        });
    }

    /*package*/ final void sendFinallyEvent(final Response response) {
        execute(() -> onFinally(response));
    }

    /*package*/ final synchronized void sendCancelEvent() {
        if (isCanceled){
            return;
        }
        execute(() -> {
            AbsCallbackHandler.this.isCanceled = true;
            onCancel();
        });
    }

    private void execute(Runnable command){
        if (mExecutor == null || threadInterrupted()){
            return;
        }

        mExecutor.execute(command);
    }

    private boolean threadInterrupted(){
        return Thread.currentThread().isInterrupted();
    }

}
