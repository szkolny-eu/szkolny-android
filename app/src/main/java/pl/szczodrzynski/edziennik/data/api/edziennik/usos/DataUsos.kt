/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_USOS_API
import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile

class DataUsos(
    app: App,
    profile: Profile?,
    loginStore: LoginStore,
) : Data(app, profile, loginStore) {

    fun isApiLoginValid() = oauthTokenKey != null && oauthTokenSecret != null

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isApiLoginValid()) {
            loginMethods += LOGIN_METHOD_USOS_API
        }
    }

    override fun generateUserCode() = "USOS:TEST"

    var instanceUrl: String?
        get() { mInstanceUrl = mInstanceUrl ?: loginStore.getLoginData("instanceUrl", null); return mInstanceUrl }
        set(value) { loginStore.putLoginData("instanceUrl", value); mInstanceUrl = value }
    private var mInstanceUrl: String? = null

    var oauthConsumerKey: String?
        get() { mOauthConsumerKey = mOauthConsumerKey ?: loginStore.getLoginData("oauthConsumerKey", null); return mOauthConsumerKey }
        set(value) { loginStore.putLoginData("oauthConsumerKey", value); mOauthConsumerKey = value }
    private var mOauthConsumerKey: String? = null

    var oauthConsumerSecret: String?
        get() { mOauthConsumerSecret = mOauthConsumerSecret ?: loginStore.getLoginData("oauthConsumerSecret", null); return mOauthConsumerSecret }
        set(value) { loginStore.putLoginData("oauthConsumerSecret", value); mOauthConsumerSecret = value }
    private var mOauthConsumerSecret: String? = null

    var oauthTokenKey: String?
        get() { mOauthTokenKey = mOauthTokenKey ?: loginStore.getLoginData("oauthTokenKey", null); return mOauthTokenKey }
        set(value) { loginStore.putLoginData("oauthTokenKey", value); mOauthTokenKey = value }
    private var mOauthTokenKey: String? = null

    var oauthTokenSecret: String?
        get() { mOauthTokenSecret = mOauthTokenSecret ?: loginStore.getLoginData("oauthTokenSecret", null); return mOauthTokenSecret }
        set(value) { loginStore.putLoginData("oauthTokenSecret", value); mOauthTokenSecret = value }
    private var mOauthTokenSecret: String? = null

    var studentId: String?
        get() { mStudentId = mStudentId ?: profile?.getStudentData("studentId", null); return mStudentId }
        set(value) { profile?.putStudentData("studentId", value) ?: return; mStudentId = value }
    private var mStudentId: String? = null

    var studentNumber: String?
        get() { mStudentNumber = mStudentNumber ?: profile?.getStudentData("studentNumber", null); return mStudentNumber }
        set(value) { profile?.putStudentData("studentNumber", value) ?: return; mStudentNumber = value }
    private var mStudentNumber: String? = null
}
