/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-17
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.gdynia

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_GDYNIA_WEB
import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile

class DataGdynia(app: App, profile: Profile?, loginStore: LoginStore) : Data(app, profile, loginStore) {

    fun isWebLoginValid() = false

    override fun satisfyLoginMethods() {
        loginMethods.clear()
        if (isWebLoginValid())
            loginMethods += LOGIN_METHOD_GDYNIA_WEB
    }

    override fun generateUserCode(): String {
        TODO("Not yet implemented")
    }

    private var mLoginUsername: String? = null
    var loginUsername: String?
        get() { mLoginUsername = mLoginUsername ?: loginStore.getLoginData("username", null); return mLoginUsername }
        set(value) { loginStore.putLoginData("username", value); mLoginUsername = value }

    private var mLoginPassword: String? = null
    var loginPassword: String?
        get() { mLoginPassword = mLoginPassword ?: loginStore.getLoginData("password", null); return mLoginPassword }
        set(value) { loginStore.putLoginData("password", value); mLoginPassword = value }

    /*   __          __  _
         \ \        / / | |
          \ \  /\  / /__| |__
           \ \/  \/ / _ \ '_ \
            \  /\  /  __/ |_) |
             \/  \/ \___|_._*/

    /*
                        .-.
                _.--"""".o/         .-.-._
             __'   ."""; {        _J ,__  `.
            ; o\.-.`._.'J;       ; /  `- /  ;
            `--i`". `" .';       `._ __.'   |
                \  `"""   \         `;      :
                 `."-.     ;     ____/     /
                   `-.`     `-.-'    `"-..'
     ___              `;__.-'"           `.
  .-{_  `--._         /.-"                 `-.
 /    ""T    ""---...'  _.-""   """-.         `.
;       /                 __.-"".    `.         `,             _..
 \     /            __.-""       '.    \          `.,__      .'L' }
  `---"`-.__    __."    .-.       j     `.         :   `.  .' ,' /
            """"       /   \     :        `.       |     F' \   ;
                      ;     `-._,L_,-""-.   `-,    ;     `   ; /
                       `.       7        `-._  `.__/_        \/
                         \     _;            \  _.'  `-.     /
                          `---" `.___,,      ;""        \  .'
                                    _/       ;           `"
                                 .-"     _,-'
                                {       "";
                                 ;-.____.'`.
                                  `.  \ '.  :
                                    \  : : /
                                     `':*/
    private var mWebSid: String? = null
    var webSid: String?
        get() { mWebSid = mWebSid ?: loginStore.getLoginData("webSid", null); return mWebSid }
        set(value) { loginStore.putLoginData("webSid", value); mWebSid = value }
}
