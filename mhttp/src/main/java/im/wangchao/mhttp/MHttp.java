package im.wangchao.mhttp;

import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import androidx.annotation.NonNull;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;

import im.wangchao.mhttp.internal.interceptor.HttpLoggingInterceptor;
import im.wangchao.mhttp.internal.interceptor.MBridgeInterceptor;
import im.wangchao.mhttp.internal.log.LoggerImpl;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * <p>Description  : MHttp.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/6/2.</p>
 * <p>Time         : 上午8:40.</p>
 */
public final class MHttp {
    private static volatile MHttp instance;

    private OkHttpClient.Builder mOkBuilder;
    private OkHttpClient mInnerClient;
    private URLInterceptor mURLInterceptor;
    private final HttpLoggingInterceptor mLoggingInterceptor = new HttpLoggingInterceptor(LoggerImpl.instance.get());

    public static MHttp instance(){
        if (instance == null) {
            synchronized (MHttp.class){
                instance = new MHttp();
            }
        }
        return instance;
    }

    /**
     * Create annotation api
     */
    public static <T> T create(Class<T> api){
        return BindApi.bind(api);
    }

    /**
     * OkHttpClient
     */
    public MHttp customOkHttpClient(@NonNull OkHttpClient client){
        mOkBuilder = client.newBuilder()
                .addInterceptor(MBridgeInterceptor.instance.get());
        return this;
    }

    /**
     * Set logging level
     */
    public MHttp loggingLevel(HttpLoggingInterceptor.Level level){
        mLoggingInterceptor.setLevel(level);
        LoggerImpl.instance.get().setLevel(level);
        if (!mOkBuilder.interceptors().contains(mLoggingInterceptor)) {
            mOkBuilder.addInterceptor(mLoggingInterceptor);
        }
        return this;
    }

    /**
     * Set cache Dir
     */
    public MHttp cache(Context context, String dirName) {
        File cache = new File(context.getApplicationContext().getCacheDir(), dirName);
        if (!cache.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cache.mkdirs();
        }
        long size = 5 * 1024 * 1024;
        try {
            StatFs statFs = new StatFs(cache.getAbsolutePath());
            long count, blockSize;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1){
                count = statFs.getBlockCountLong();
                blockSize = statFs.getBlockSizeLong();
            } else {
                count = statFs.getBlockCount();
                blockSize = statFs.getBlockSize();
            }
            long available = count * blockSize;
            // Target 2% of the total space.
            size = available / 50;
        } catch (IllegalArgumentException ignored) {
        }
        // Bound inside min/max size for disk cache.
        size = Math.max(Math.min(size, size * 10), size);

        mOkBuilder.cache(new Cache(cache, size));
        return this;
    }

    /**
     * Set connect, read and write time with {@link TimeUnit#SECONDS}
     */
    public MHttp timeout(int timeout){
        timeout(timeout, TimeUnit.SECONDS);
        return this;
    }

    public MHttp timeout(int timeout, TimeUnit unit){
        connectTimeout(timeout, unit);
        readTimeout(timeout, unit);
        writeTimeout(timeout, unit);
        return this;
    }

    public MHttp connectTimeout(long timeout, TimeUnit unit){
        mOkBuilder.connectTimeout(timeout, unit);
        return this;
    }

    public MHttp readTimeout(long timeout, TimeUnit unit){
        mOkBuilder.readTimeout(timeout, unit);
        return this;
    }

    public MHttp writeTimeout(long timeout, TimeUnit unit){
        mOkBuilder.writeTimeout(timeout, unit);
        return this;
    }

    public MHttp setURLInterceptor(URLInterceptor interceptor){
        this.mURLInterceptor = interceptor;
        return this;
    }

    /**
     * Trust all certificate for debug
     */
    public MHttp trustAllCertificate(){
        HTTPS.trustAllCertificate(mOkBuilder);
        return this;
    }

    /**
     * Set Certificate
     */
    public MHttp setCertificates(InputStream... certificates){
        return setCertificates(certificates, null, null);
    }

    /**
     * Set Certificate
     */
    public MHttp setCertificates(X509TrustManager trustManager, InputStream bksFile, String password) {
        try {
            HTTPS.setCertificates(mOkBuilder, trustManager, null, bksFile, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Set Certificate
     */
    public MHttp setCertificates(InputStream[] certificates, InputStream bksFile, String password) {
        try {
            HTTPS.setCertificates(mOkBuilder, null, certificates, bksFile, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public MHttp hostnameVerifier(HostnameVerifier hostnameVerifier){
        mOkBuilder.hostnameVerifier(hostnameVerifier);
        return this;
    }

    /**
     * @return Current client.
     */
    public OkHttpClient client(){
        if (mOkBuilder == null){
            throw new IllegalArgumentException("OkHttpClient cannot be null, please call the MHttp#client(OkHttpClient client) method first.");
        }
        if (mInnerClient == null){
            mInnerClient = mOkBuilder.build();
        }
        return mInnerClient;
    }

    /**
     * Cancel all request.
     */
    public MHttp cancelAll(){
        client().dispatcher().cancelAll();
        return this;
    }

    /**
     * Cancel request with {@code tag}
     */
    public MHttp cancel(Object tag){
        for (Call call : client().dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
        for (Call call : client().dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
        return this;
    }

    /*package*/ String proceedURL(String url){
        if (mURLInterceptor != null){
            return mURLInterceptor.interceptor(url);
        }
        return url;
    }

    /*package*/ HttpUrl proceedURL(HttpUrl url){
        if (mURLInterceptor != null){
            return mURLInterceptor.interceptor(url);
        }
        return url;
    }

    /*package*/ URL proceedURL(URL url){
        if (mURLInterceptor != null){
            return mURLInterceptor.interceptor(url);
        }
        return url;
    }

    //@private
    private MHttp(){
        //default instance
        mOkBuilder = new OkHttpClient.Builder()
                .addInterceptor(MBridgeInterceptor.instance.get());
    }

}
