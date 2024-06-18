package pl.szczodrzynski.navlib.drawer

interface IUnreadCounter {
    var profileId: Int
    var type: Int
    var drawerItemId: Int?
    var count: Int
}