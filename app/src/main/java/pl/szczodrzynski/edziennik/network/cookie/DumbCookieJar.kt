/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-24.
 */

package pl.szczodrzynski.edziennik.network.cookie

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * A simple cookie jar that does not care about the [Cookie.secure], [Cookie.hostOnly],
 * [Cookie.httpOnly] and [Cookie.path] attributes.
 */
class DumbCookieJar(
        /**
         * A context to create the shared prefs file.
         */
        context: Context,

        /**
         * Whether to persist session cookies as well, when [Cookie.persistent] is false.
         */
        private val persistAll: Boolean = false
) : CookieJar {

    private val prefs = context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
    private val sessionCookies = mutableSetOf<DumbCookie>()
    private val savedCookies = mutableSetOf<DumbCookie>()
    private fun save(dc: DumbCookie) {
        sessionCookies.remove(dc)
        sessionCookies.add(dc)
        if (dc.cookie.persistent() || persistAll) {
            savedCookies.remove(dc)
            savedCookies.add(dc)
        }
    }
    private fun delete(vararg toRemove: DumbCookie) {
        sessionCookies.removeAll(toRemove)
        savedCookies.removeAll(toRemove)
    }

    override fun saveFromResponse(url: HttpUrl?, cookies: List<Cookie>) {
        for (cookie in cookies) {
            val dc = DumbCookie(cookie)
            save(dc)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return sessionCookies.filter {
            it.cookie.matches(url)
        }.map { it.cookie }
    }

    fun get(domain: String, name: String): String? {
        return sessionCookies.firstOrNull {
            it.domainMatches(domain) && it.cookie.name() == name
        }?.cookie?.value()
    }

    fun set(domain: String, name: String, value: String?, isSession: Boolean) = set(
            domain, name, value,
            if (isSession) null
            else System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L
    )

    /**
     * Add a cookie to the cache.
     * By default a session cookie is added. If [expiresAt] is set, the cookie is
     * additionally persisted.
     */
    fun set(domain: String, name: String?, value: String?, expiresAt: Long? = null) {
        name ?: return
        if (value == null) {
            remove(domain, name)
            return
        }
        val dc = DumbCookie(domain, name, value, expiresAt)
        save(dc)
    }

    fun getAll(domain: String): Map<String, String> {
        return sessionCookies.filter {
            it.domainMatches(domain)
        }.map { it.cookie.name() to it.cookie.value() }.toMap()
    }

    fun remove(domain: String, name: String) {
        val toRemove = sessionCookies.filter {
            it.domainMatches(domain) && it.cookie.name() == name
        }
        delete(*toRemove.toTypedArray())
    }

    fun clear(domain: String) {
        val toRemove = sessionCookies.filter {
            it.domainMatches(domain)
        }
        delete(*toRemove.toTypedArray())
    }
}
