package im.wangchao.mhttp.rxjava2;

import im.wangchao.mhttp.AbsCallbackHandler;
import im.wangchao.mhttp.Request;
import im.wangchao.mhttp.Response;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * <p>Description  : ResponseEnqueueObservable.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/3/19.</p>
 * <p>Time         : 下午4:45.</p>
 */
public class ResponseEnqueueObservable<T> extends Observable<T> {
    private final RxRequest<T> request;

    public ResponseEnqueueObservable(RxRequest<T> request){
        this.request = request;
    }

    @Override protected void subscribeActual(Observer<? super T> observer) {

        AbsCallbackHandler<T> callback = request.callback();
        EnqueueDisposable<T> disposable = new EnqueueDisposable<>(observer, callback);

        observer.onSubscribe(disposable);
        request.request().newBuilder().callback(disposable).build().enqueue();
    }

    private static final class EnqueueDisposable<T> extends AbsCallbackHandler<T> implements Disposable{
        private final AbsCallbackHandler<T> originCallback;
        private final Observer<? super T> observer;
        private volatile boolean disposed;
        boolean terminated = false;

        EnqueueDisposable(Observer<? super T> observer, AbsCallbackHandler<T> callbackHandler){
            this.observer = observer;
            this.originCallback = callbackHandler;
        }

        @Override public void dispose() {
            disposed = true;
            Request request = getRequest();
            request.cancel();
        }

        @Override public boolean isDisposed() {
            return disposed;
        }


        @Override public String charset() {
            return originCallback.charset();
        }

        @Override public String accept() {
            return originCallback.accept();
        }

        @Override public void onStart() {
            originCallback.onStart();
        }

        @Override public void onUploadProgress(int bytesWritten, int bytesTotal) {
            originCallback.onUploadProgress(bytesWritten, bytesTotal);
        }

        @Override public void onProgress(long bytesWritten, long bytesTotal) {
            originCallback.onProgress(bytesWritten, bytesTotal);
        }

        @Override public void onFinish() {
            originCallback.onFinish();
        }

        @Override public void onCancel() {
            originCallback.onCancel();
            if (!disposed){
                dispose();
            }
        }

        @Override public T apply(Response response) throws Exception {
            return originCallback.apply(response);
        }

        @Override public void onSuccess(T data, Response response) {
            if (disposed) return;

            try {
                originCallback.onSuccess(data, response);

                observer.onNext(data);

                if (!disposed) {
                    terminated = true;
                    observer.onComplete();
                }
            } catch (Throwable t) {
                if (terminated) {
                    RxJavaPlugins.onError(t);
                } else if (!disposed) {
                    try {
                        observer.onError(t);
                    } catch (Throwable inner) {
                        Exceptions.throwIfFatal(inner);
                        RxJavaPlugins.onError(new CompositeException(t, inner));
                    }
                }
            }
        }

        @Override public void onFailure(Response response, Throwable throwable) {
            try {
                originCallback.onFailure(response, throwable);

                observer.onError(throwable);
            } catch (Throwable inner) {
                Exceptions.throwIfFatal(inner);
                RxJavaPlugins.onError(new CompositeException(throwable, inner));
            }
        }

        @Override public void onFinally(Response response) {
            originCallback.onFinally(response);
        }
    }
}
