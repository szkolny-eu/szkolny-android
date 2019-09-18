package im.wangchao.mhttp.internal.cookie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import im.wangchao.mhttp.internal.cookie.cache.CookieCache;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * <p>Description  : MemeryCookieJar.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 16/3/18.</p>
 * <p>Time         : 下午2:20.</p>
 */
public class MemoryCookieJar implements ClearableCookieJar {

    private CookieCache cache;

    public MemoryCookieJar(CookieCache cache) {
        this.cache = cache;
    }

    @Override
    synchronized public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        cache.addAll(cookies);
    }

    @Override
    synchronized public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> validCookies = new ArrayList<>();

        for (Iterator<Cookie> it = cache.iterator(); it.hasNext(); ) {
            Cookie currentCookie = it.next();

            if (isCookieExpired(currentCookie)) {
                it.remove();

            } else if (currentCookie.matches(url)) {
                validCookies.add(currentCookie);
            }
        }


        return  validCookies;
    }

    private static boolean isCookieExpired(Cookie cookie) {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    synchronized public void clear() {
        cache.clear();
    }

    synchronized public void clearForDomain(String domain) {
        cache.clearForDomain(domain);
    }
}
