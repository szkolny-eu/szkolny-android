# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform

# This is added for okhttp 3.1.2 bug fix as shown at https://github.com/square/okhttp/issues/2323
-keepclassmembers class * implements javax.net.ssl.SSLSocketFactory {
     private javax.net.ssl.SSLSocketFactory delegate;
}

-keepnames class sun.security.ssl.SSLContextImpl
-keepnames class javax.net.ssl.SSLSocketFactory
