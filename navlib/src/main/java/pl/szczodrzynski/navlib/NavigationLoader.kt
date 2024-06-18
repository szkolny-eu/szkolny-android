package pl.szczodrzynski.navlib

interface NavigationLoader {
    fun load(itemId: Int, callerId: Int, source: Int, args: Map<String, Any?>)
}