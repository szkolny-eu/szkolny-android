/*
 * Copyright (C) 2016 Francisco José Montiel Navarro.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.wangchao.mhttp.internal.cookie;


import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import im.wangchao.mhttp.internal.cookie.cache.CookieCache;
import im.wangchao.mhttp.internal.cookie.persistence.CookiePersistor;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class PersistentCookieJar implements ClearableCookieJar {

    private CookieCache cache;
    private CookiePersistor persistor;

    public PersistentCookieJar(CookieCache cache, CookiePersistor persistor) {
        this.cache = cache;
        this.persistor = persistor;

        this.cache.addAll(persistor.loadAll());
    }

    @Override
    synchronized public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        //Log.d("PersistentCookieJar", "FINISHING "+url.toString());
        // cookies need to be reversed, in order to replace old cookies with these coming later
        // (if there are duplicate cookies in the same response)
        List<Cookie> reverseCookies = new ArrayList<>(cookies);
        Collections.reverse(reverseCookies);
        /*for (Cookie cookie: reverseCookies) {
            Log.d("PersistentCookieJar", "Saving cookie "+cookie.toString()+" from URL "+url.toString());
        }*/
        cache.addAll(reverseCookies);
        persistor.saveAll(reverseCookies);
        /*Log.d("PersistentCookieJar", "Cookies saved: ");
        for (Cookie cookie : cache) {
            Log.d("PersistentCookieJar", "Saving cookie " + cookie.toString() + " from URL " + url.toString());
        }*/
    }

    @NonNull
    @Override
    synchronized public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> removedCookies = new ArrayList<>();
        List<Cookie> validCookies = new ArrayList<>();

        //Log.d("PersistentCookieJar", "REQUESTING "+url.toString());

        for (Iterator<Cookie> it = cache.iterator(); it.hasNext(); ) {
            Cookie currentCookie = it.next();
            //Log.d("PersistentCookieJar", "Loading "+currentCookie.toString()+" to URL "+url.toString());
            if (isCookieExpired(currentCookie)) {
                //Log.d("PersistentCookieJar", "Cookie expired at "+new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(currentCookie.expiresAt())));
                removedCookies.add(currentCookie);
                it.remove();

            } else if (currentCookie.matches(url)) {
                //Log.d("PersistentCookieJar", "Cookie is still valid until "+new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(currentCookie.expiresAt())));
                validCookies.add(currentCookie);
            }
            /*else {
                Log.d("PersistentCookieJar", "URL doesn't match");
            }*/
        }

        persistor.removeAll(removedCookies);

        return validCookies;
    }

    private static boolean isCookieExpired(Cookie cookie) {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    synchronized public void clear() {
        cache.clear();
        persistor.clear();
    }

    synchronized public void clearForDomain(String domain) {
        cache.clearForDomain(domain);
        persistor.clearForDomain(domain);
    }
}
