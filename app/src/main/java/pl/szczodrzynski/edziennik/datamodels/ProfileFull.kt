package pl.szczodrzynski.edziennik.datamodels

import android.content.Context
import androidx.room.ColumnInfo
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_AGENDA
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_ANNOUNCEMENTS
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_ATTENDANCES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_GRADES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_HOMEWORK
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_NOTICES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_TIMETABLE
import pl.szczodrzynski.edziennik.datamodels.LoginStore.*
import java.util.*

class ProfileFull : Profile {
    @ColumnInfo(name = "loginStoreType")
    var loginStoreType: Int = 0
    @ColumnInfo(name = "loginStoreData")
    var loginStoreData: JsonObject? = null

    val usernameId: String
        get() {
            if (loginStoreData == null) {
                return "NO_LOGIN_STORE"
            }
            if (studentData == null) {
                return "NO_STUDENT_STORE"
            }
            return when (loginStoreType) {
                LOGIN_TYPE_MOBIDZIENNIK -> getLoginData("serverName", "MOBI_UN") + ":" + getLoginData("username", "MOBI_UN") + ":" + getStudentData("studentId", -1)
                LOGIN_TYPE_LIBRUS -> getStudentData("schoolName", "LIBRUS_UN") + ":" + getStudentData("accountLogin", "LIBRUS_LOGIN_UN")
                LOGIN_TYPE_IUCZNIOWIE -> getLoginData("schoolName", "IUCZNIOWIE_UN") + ":" + getLoginData("username", "IUCZNIOWIE_UN") + ":" + getStudentData("registerId", -1)
                LOGIN_TYPE_VULCAN -> getStudentData("schoolName", "VULCAN_UN") + ":" + getStudentData("studentId", -1)
                LOGIN_TYPE_DEMO -> getLoginData("serverName", "DEMO_UN") + ":" + getLoginData("username", "DEMO_UN") + ":" + getStudentData("studentId", -1)
                else -> "TYPE_UNKNOWN"
            }
        }

    // example (minimal) list of fragments
    // there will never be less available options
    val supportedFragments: List<Int>
        get() {
            val fragmentIds: MutableList<Int>
            when (loginStoreType) {
                LOGIN_TYPE_MOBIDZIENNIK, LOGIN_TYPE_DEMO, LOGIN_TYPE_VULCAN -> {
                    fragmentIds = ArrayList()
                    fragmentIds.add(DRAWER_ITEM_TIMETABLE)
                    fragmentIds.add(DRAWER_ITEM_AGENDA)
                    fragmentIds.add(DRAWER_ITEM_GRADES)
                    fragmentIds.add(DRAWER_ITEM_MESSAGES)
                    fragmentIds.add(DRAWER_ITEM_HOMEWORK)
                    fragmentIds.add(DRAWER_ITEM_NOTICES)
                    fragmentIds.add(DRAWER_ITEM_ATTENDANCES)
                    return fragmentIds
                }
                LOGIN_TYPE_LIBRUS -> {
                    fragmentIds = ArrayList()
                    fragmentIds.add(DRAWER_ITEM_TIMETABLE)
                    fragmentIds.add(DRAWER_ITEM_AGENDA)
                    fragmentIds.add(DRAWER_ITEM_GRADES)
                    fragmentIds.add(DRAWER_ITEM_MESSAGES)
                    fragmentIds.add(DRAWER_ITEM_HOMEWORK)
                    fragmentIds.add(DRAWER_ITEM_NOTICES)
                    fragmentIds.add(DRAWER_ITEM_ATTENDANCES)
                    fragmentIds.add(DRAWER_ITEM_ANNOUNCEMENTS)
                    return fragmentIds
                }
                LOGIN_TYPE_IUCZNIOWIE -> {
                    fragmentIds = ArrayList()
                    fragmentIds.add(DRAWER_ITEM_TIMETABLE)
                    fragmentIds.add(DRAWER_ITEM_AGENDA)
                    fragmentIds.add(DRAWER_ITEM_GRADES)
                    fragmentIds.add(DRAWER_ITEM_MESSAGES)
                    fragmentIds.add(DRAWER_ITEM_NOTICES)
                    fragmentIds.add(DRAWER_ITEM_ATTENDANCES)
                    fragmentIds.add(DRAWER_ITEM_ANNOUNCEMENTS)
                    return fragmentIds
                }
            }
            fragmentIds = ArrayList()
            fragmentIds.add(DRAWER_ITEM_TIMETABLE)
            fragmentIds.add(DRAWER_ITEM_AGENDA)
            fragmentIds.add(DRAWER_ITEM_GRADES)
            return fragmentIds
        }

    constructor() : super() {

    }

    constructor(profile: Profile, loginStore: LoginStore) {

        /*Profile::class.memberProperties.forEach { profileProperty ->
            if (profileProperty.visibility == KVisibility.PUBLIC) {
                ProfileFull::class.memberProperties.singleOrNull { it.name == profileProperty.name }?.let { fullProperty ->
                    if (fullProperty is KMutableProperty<*>) {
                        fullProperty.setter.call(this, profileProperty.get(profile))
                    }
                }
            }
        }*/

        Profile::class.java.declaredFields.forEach { profileProperty ->
            profileProperty.isAccessible = true
            profileProperty.set(this, profileProperty.get(profile))
        }

        this.loginStoreType = loginStore.type
        this.loginStoreId = loginStore.id
        this.loginStoreData = loginStore.data
        /*for (Field field: LoginStore.class.getFields()) {
            try {
                ProfileFull.class.getField(field.getName()).set(this, field.get(loginStore));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }*/
    }

    constructor(context: Context) : super(context) {}

    fun canChangeLoginPassword(): Boolean {
        return loginStoreType == LOGIN_TYPE_MOBIDZIENNIK || loginStoreType == LOGIN_TYPE_LIBRUS || loginStoreType == LOGIN_TYPE_IUCZNIOWIE
    }

    fun changeLoginPassword(password: String) {
        if (loginStoreData == null) {
            return
        }
        if (studentData == null) {
            return
        }
        when (loginStoreType) {
            LOGIN_TYPE_MOBIDZIENNIK, LOGIN_TYPE_LIBRUS, LOGIN_TYPE_IUCZNIOWIE -> putLoginData("password", password)
        }
    }


    fun getLoginData(key: String, defaultValue: String): String {
        if (loginStoreData == null)
            return defaultValue
        val element = loginStoreData!!.get(key)
        return if (element != null) {
            element.asString
        } else defaultValue
    }

    fun getLoginData(key: String, defaultValue: Int): Int {
        if (loginStoreData == null)
            return defaultValue
        val element = loginStoreData!!.get(key)
        return element?.asInt ?: defaultValue
    }

    fun getLoginData(key: String, defaultValue: Long): Long {
        if (loginStoreData == null)
            return defaultValue
        val element = loginStoreData!!.get(key)
        return element?.asLong ?: defaultValue
    }

    fun getLoginData(key: String, defaultValue: Float): Float {
        if (loginStoreData == null)
            return defaultValue
        val element = loginStoreData!!.get(key)
        return element?.asFloat ?: defaultValue
    }

    fun getLoginData(key: String, defaultValue: Boolean): Boolean {
        if (loginStoreData == null)
            return defaultValue
        val element = loginStoreData!!.get(key)
        return element?.asBoolean ?: defaultValue
    }

    fun putLoginData(key: String, value: String) {
        forceLoginStore()
        loginStoreData!!.addProperty(key, value)
    }

    fun putLoginData(key: String, value: Int) {
        forceLoginStore()
        loginStoreData!!.addProperty(key, value)
    }

    fun putLoginData(key: String, value: Long) {
        forceLoginStore()
        loginStoreData!!.addProperty(key, value)
    }

    fun putLoginData(key: String, value: Float) {
        forceLoginStore()
        loginStoreData!!.addProperty(key, value)
    }

    fun putLoginData(key: String, value: Boolean) {
        forceLoginStore()
        loginStoreData!!.addProperty(key, value)
    }

    fun removeLoginData(key: String) {
        if (loginStoreData == null)
            return
        loginStoreData!!.remove(key)
    }

    fun clearLoginStore() {
        loginStoreData = JsonObject()
    }

    private fun forceLoginStore() {
        if (loginStoreData == null) {
            clearLoginStore()
        }
    }

    fun loginStoreType(): String {
        when (loginStoreType) {
            LOGIN_TYPE_MOBIDZIENNIK -> return "LOGIN_TYPE_MOBIDZIENNIK"
            LOGIN_TYPE_LIBRUS -> return "LOGIN_TYPE_LIBRUS"
            LOGIN_TYPE_IUCZNIOWIE -> return "LOGIN_TYPE_IUCZNIOWIE"
            LOGIN_TYPE_VULCAN -> return "LOGIN_TYPE_VULCAN"
            LOGIN_TYPE_DEMO -> return "LOGIN_TYPE_DEMO"
            else -> return "LOGIN_TYPE_UNKNOWN"
        }
    }

    override fun toString(): String {
        return "ProfileFull{" +
                "parent=" + super.toString() +
                "loginStoreId=" + loginStoreId +
                ", loginStoreType=" + loginStoreType() +
                ", loginStoreData=" + loginStoreData +
                '}'.toString()
    }
}
