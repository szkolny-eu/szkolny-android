package im.wangchao.mhttp;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.internal.Util;

/**
 * <p>Description  : HTTPS.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/9/2.</p>
 * <p>Time         : 下午3:58.</p>
 */
/*package*/ final class HTTPS {

    // 信任所有证书的 TrustManager
    private static X509TrustManager TrustAllCertificate = new X509TrustManager() {
        @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }
    };

    /**
     * Trust all certificate for debug
     */
    /*package*/ static void trustAllCertificate(OkHttpClient.Builder builder) {
        builder.sslSocketFactory(new Android5SSL(TrustAllCertificate), TrustAllCertificate);
    }

    /**
     * Set Certificate
     */
    /*package*/ static void setCertificates(OkHttpClient.Builder builder,
                                            InputStream... certificates) throws Exception {
        setCertificates(builder, null, certificates, null, null);
    }

    /**
     * Set Certificate
     */
    /*package*/ static void setCertificates(OkHttpClient.Builder builder,
                                            X509TrustManager trustManager,
                                            InputStream bksFile,
                                            String password) throws Exception {
        setCertificates(builder, trustManager, null, bksFile, password);
    }

    /**
     * Set Certificate
     */
    /*package*/ static void setCertificates(OkHttpClient.Builder builder,
                                            InputStream[] certificates,
                                            InputStream bksFile,
                                            String password) throws Exception {
        setCertificates(builder, null, certificates, bksFile, password);
    }

    /**
     * Set Certificate
     */
    /*package*/ static void setCertificates(OkHttpClient.Builder builder,
                                            X509TrustManager trustManager,
                                            InputStream[] certificates,
                                            InputStream bksFile,
                                            String password) throws Exception {
        TrustManager[] trustManagers = prepareTrustManager(certificates);
        KeyManager[] keyManagers = prepareKeyManager(bksFile, password);

        X509TrustManager manager;
        if (trustManager != null) {
            manager = trustManager;
        } else if (trustManagers != null) {
            manager = chooseTrustManager(trustManagers);
        } else {
            manager = TrustAllCertificate;
        }

        // 创建TLS类型的SSLContext对象， that uses our finalTrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        // 用上面得到的trustManagers初始化SSLContext，这样sslContext就会信任keyStore中的证书
        // 第一个参数是授权的密钥管理器，用来授权验证，比如授权自签名的证书验证。第二个是被授权的证书管理器，用来验证服务器端的证书
        sslContext.init(keyManagers, new TrustManager[]{manager}, new SecureRandom());
        builder.sslSocketFactory(sslContext.getSocketFactory(), manager);
    }

    /*package*/ static TrustManager[] prepareTrustManager(InputStream... certificates) throws Exception {
        if (certificates == null || certificates.length <= 0) return null;

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        // 创建一个默认类型的KeyStore，存储我们信任的证书
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        int index = 0;
        for (InputStream certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
            Util.closeQuietly(certificate);
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.
                getInstance(TrustManagerFactory.getDefaultAlgorithm());
        //用我们之前的keyStore实例初始化TrustManagerFactory，使TrustManagerFactory信任keyStore中的证书
        trustManagerFactory.init(keyStore);
        return trustManagerFactory.getTrustManagers();
    }

    /*package*/ static KeyManager[] prepareKeyManager(InputStream bksFile, String password) throws Exception{
        if (bksFile == null || password == null) return null;

        KeyStore clientKeyStore = KeyStore.getInstance("BKS");
        clientKeyStore.load(bksFile, password.toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, password.toCharArray());
        return keyManagerFactory.getKeyManagers();
    }

    /*package*/ static X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
    }

    /*                  X509TrustManager                  */
    /*package*/ static class MyTrustManager implements X509TrustManager {
        private X509TrustManager defaultTrustManager;
        private X509TrustManager localTrustManager;

        /*package*/ MyTrustManager(X509TrustManager localTrustManager) throws NoSuchAlgorithmException, KeyStoreException {
            TrustManagerFactory var4 = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            var4.init((KeyStore) null);
            defaultTrustManager = chooseTrustManager(var4.getTrustManagers());
            this.localTrustManager = localTrustManager;
        }

        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
//            try {
//                defaultTrustManager.checkClientTrusted(chain, authType);
//            } catch (CertificateException e) {
//                localTrustManager.checkClientTrusted(chain, authType);
//            }
        }

        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                localTrustManager.checkServerTrusted(chain, authType);
            }
        }


        @Override public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
