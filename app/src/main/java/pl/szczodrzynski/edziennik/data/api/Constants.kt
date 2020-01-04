/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-19.
 */

package pl.szczodrzynski.edziennik.data.api

import android.os.Build
import pl.szczodrzynski.edziennik.BuildConfig

const val GET = 0
const val POST = 1

val SYSTEM_USER_AGENT = System.getProperty("http.agent") ?: "Dalvik/2.1.0 Android"

val SERVER_USER_AGENT = "Szkolny.eu/${BuildConfig.VERSION_NAME} $SYSTEM_USER_AGENT"

const val FAKE_LIBRUS_API = "https://librus.szkolny.eu/api"
const val FAKE_LIBRUS_PORTAL = "https://librus.szkolny.eu"
const val FAKE_LIBRUS_AUTHORIZE = "https://librus.szkolny.eu/authorize.php"
const val FAKE_LIBRUS_LOGIN = "https://librus.szkolny.eu/login_action.php"
const val FAKE_LIBRUS_TOKEN = "https://librus.szkolny.eu/access_token.php"
const val FAKE_LIBRUS_ACCOUNT = "/synergia_accounts_fresh.php?login="
const val FAKE_LIBRUS_ACCOUNTS = "/synergia_accounts.php"

val LIBRUS_USER_AGENT = "$SYSTEM_USER_AGENT LibrusMobileApp"
const val SYNERGIA_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Gecko/20100101 Firefox/62.0"
const val LIBRUS_CLIENT_ID = "wmSyUMo8llDAs4y9tJVYY92oyZ6h4lAt7KCuy0Gv"
const val LIBRUS_REDIRECT_URL = "http://localhost/bar"
const val LIBRUS_AUTHORIZE_URL = "https://portal.librus.pl/oauth2/authorize?client_id=$LIBRUS_CLIENT_ID&redirect_uri=$LIBRUS_REDIRECT_URL&response_type=code"
const val LIBRUS_LOGIN_URL = "https://portal.librus.pl/rodzina/login/action"
const val LIBRUS_TOKEN_URL = "https://portal.librus.pl/oauth2/access_token"

const val LIBRUS_ACCOUNT_URL = "/v2/SynergiaAccounts/fresh/" // + login
const val LIBRUS_ACCOUNTS_URL = "/v2/SynergiaAccounts"

/** https://api.librus.pl/2.0 */
const val LIBRUS_API_URL = "https://api.librus.pl/2.0"
/** https://portal.librus.pl/api */
const val LIBRUS_PORTAL_URL = "https://portal.librus.pl/api"
/** https://api.librus.pl/OAuth/Token */
const val LIBRUS_API_TOKEN_URL = "https://api.librus.pl/OAuth/Token"
/** https://api.librus.pl/OAuth/TokenJST */
const val LIBRUS_API_TOKEN_JST_URL = "https://api.librus.pl/OAuth/TokenJST"
const val LIBRUS_API_AUTHORIZATION = "Mjg6ODRmZGQzYTg3YjAzZDNlYTZmZmU3NzdiNThiMzMyYjE="
const val LIBRUS_API_SECRET_JST = "18b7c1ee08216f636a1b1a2440e68398"
const val LIBRUS_API_CLIENT_ID_JST = "49"
//const val LIBRUS_API_CLIENT_ID_JST_REFRESH = "42"

const val LIBRUS_JST_DEMO_CODE = "68656A21"
const val LIBRUS_JST_DEMO_PIN = "1290"

const val LIBRUS_SYNERGIA_URL = "https://synergia.librus.pl"
/** https://synergia.librus.pl/loguj/token/TOKEN/przenies */
const val LIBRUS_SYNERGIA_TOKEN_LOGIN_URL = "https://synergia.librus.pl/loguj/token/TOKEN/przenies"

const val LIBRUS_MESSAGES_URL = "https://wiadomosci.librus.pl/module"
const val LIBRUS_SANDBOX_URL = "https://sandbox.librus.pl/index.php?action="

const val IDZIENNIK_USER_AGENT = SYNERGIA_USER_AGENT
const val IDZIENNIK_WEB_URL = "https://iuczniowie.progman.pl/idziennik"
const val IDZIENNIK_WEB_LOGIN = "login.aspx"
const val IDZIENNIK_WEB_SETTINGS = "mod_panelRodzica/Ustawienia.aspx"
const val IDZIENNIK_WEB_TIMETABLE = "mod_panelRodzica/plan/WS_Plan.asmx/pobierzPlanZajec"
const val IDZIENNIK_WEB_GRADES = "mod_panelRodzica/oceny/WS_ocenyUcznia.asmx/pobierzOcenyUcznia"
const val IDZIENNIK_WEB_MISSING_GRADES = "mod_panelRodzica/brak_ocen/WS_BrakOcenUcznia.asmx/pobierzBrakujaceOcenyUcznia"
const val IDZIENNIK_WEB_EXAMS = "mod_panelRodzica/sprawdziany/mod_sprawdzianyPanel.asmx/pobierzListe"
const val IDZIENNIK_WEB_HOMEWORK = "mod_panelRodzica/pracaDomowa/WS_pracaDomowa.asmx/pobierzPraceDomowe"
const val IDZIENNIK_WEB_NOTICES = "mod_panelRodzica/uwagi/WS_uwagiUcznia.asmx/pobierzUwagiUcznia"
const val IDZIENNIK_WEB_ATTENDANCE = "mod_panelRodzica/obecnosci/WS_obecnosciUcznia.asmx/pobierzObecnosciUcznia"
const val IDZIENNIK_WEB_ANNOUNCEMENTS = "mod_panelRodzica/tabOgl/WS_tablicaOgloszen.asmx/GetOgloszenia"
const val IDZIENNIK_WEB_MESSAGES_LIST = "mod_komunikator/WS_wiadomosci.asmx/PobierzListeWiadomosci"
const val IDZIENNIK_WEB_GET_MESSAGE = "mod_komunikator/WS_wiadomosci.asmx/PobierzWiadomosc"
const val IDZIENNIK_WEB_GET_RECIPIENT_LIST = "mod_komunikator/WS_wiadomosci.asmx/pobierzListeOdbiorcowPanelRodzic"
const val IDZIENNIK_WEB_SEND_MESSAGE = "mod_komunikator/WS_wiadomosci.asmx/WyslijWiadomosc"
const val IDZIENNIK_WEB_GET_ATTACHMENT = "mod_komunikator/Download.ashx"

val IDZIENNIK_API_USER_AGENT = SYSTEM_USER_AGENT
const val IDZIENNIK_API_URL = "https://iuczniowie.progman.pl/idziennik/api"
const val IDZIENNIK_API_CURRENT_REGISTER = "Uczniowie/\$STUDENT_ID/AktualnyDziennik"
const val IDZIENNIK_API_GRADES = "Uczniowie/\$STUDENT_ID/Oceny/" /* + semester */
const val IDZIENNIK_API_MESSAGES_INBOX = "Wiadomosci/Odebrane"
const val IDZIENNIK_API_MESSAGES_SENT = "Wiadomosci/Wyslane"


val MOBIDZIENNIK_USER_AGENT = SYSTEM_USER_AGENT

const val VULCAN_API_USER_AGENT = "MobileUserAgent"
const val VULCAN_API_APP_NAME = "VULCAN-Android-ModulUcznia"
const val VULCAN_API_APP_VERSION = "19.4.1.436"
const val VULCAN_API_PASSWORD = "CE75EA598C7743AD9B0B7328DED85B06"
const val VULCAN_API_PASSWORD_FAKELOG = "012345678901234567890123456789AB"
val VULCAN_API_DEVICE_NAME = "Szkolny.eu ${Build.MODEL}"

const val VULCAN_API_ENDPOINT_CERTIFICATE = "mobile-api/Uczen.v3.UczenStart/Certyfikat"
const val VULCAN_API_ENDPOINT_STUDENT_LIST = "mobile-api/Uczen.v3.UczenStart/ListaUczniow"
const val VULCAN_API_ENDPOINT_DICTIONARIES = "mobile-api/Uczen.v3.Uczen/Slowniki"
const val VULCAN_API_ENDPOINT_TIMETABLE = "mobile-api/Uczen.v3.Uczen/PlanLekcjiZeZmianami"
const val VULCAN_API_ENDPOINT_GRADES = "mobile-api/Uczen.v3.Uczen/Oceny"
const val VULCAN_API_ENDPOINT_GRADES_PROPOSITIONS = "mobile-api/Uczen.v3.Uczen/OcenyPodsumowanie"
const val VULCAN_API_ENDPOINT_EVENTS = "mobile-api/Uczen.v3.Uczen/Sprawdziany"
const val VULCAN_API_ENDPOINT_HOMEWORK = "mobile-api/Uczen.v3.Uczen/ZadaniaDomowe"
const val VULCAN_API_ENDPOINT_NOTICES = "mobile-api/Uczen.v3.Uczen/UwagiUcznia"
const val VULCAN_API_ENDPOINT_ATTENDANCE = "mobile-api/Uczen.v3.Uczen/Frekwencje"
const val VULCAN_API_ENDPOINT_MESSAGES_RECEIVED = "mobile-api/Uczen.v3.Uczen/WiadomosciOdebrane"
const val VULCAN_API_ENDPOINT_MESSAGES_SENT = "mobile-api/Uczen.v3.Uczen/WiadomosciWyslane"
const val VULCAN_API_ENDPOINT_MESSAGES_CHANGE_STATUS = "mobile-api/Uczen.v3.Uczen/ZmienStatusWiadomosci"
const val VULCAN_API_ENDPOINT_MESSAGES_ADD = "mobile-api/Uczen.v3.Uczen/DodajWiadomosc"
const val VULCAN_API_ENDPOINT_PUSH = "mobile-api/Uczen.v3.Uczen/UstawPushToken"

const val EDUDZIENNIK_USER_AGENT = "Szkolny.eu/${BuildConfig.VERSION_NAME}"
