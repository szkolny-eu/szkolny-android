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

val LIBRUS_USER_AGENT = "${SYSTEM_USER_AGENT}LibrusMobileApp"
const val SYNERGIA_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Gecko/20100101 Firefox/62.0"
const val LIBRUS_CLIENT_ID = "VaItV6oRutdo8fnjJwysnTjVlvaswf52ZqmXsJGP"
const val LIBRUS_REDIRECT_URL = "app://librus"
const val LIBRUS_AUTHORIZE_URL = "https://portal.librus.pl/oauth2/authorize?client_id=$LIBRUS_CLIENT_ID&redirect_uri=$LIBRUS_REDIRECT_URL&response_type=code"
const val LIBRUS_LOGIN_URL = "https://portal.librus.pl/rodzina/login/action"
const val LIBRUS_TOKEN_URL = "https://portal.librus.pl/oauth2/access_token"

const val LIBRUS_ACCOUNT_URL = "/v3/SynergiaAccounts/fresh/" // + login
const val LIBRUS_ACCOUNTS_URL = "/v3/SynergiaAccounts"

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
const val LIBRUS_API_CLIENT_ID_JST = "59"
//const val LIBRUS_API_CLIENT_ID_JST_REFRESH = "42"

const val LIBRUS_JST_DEMO_CODE = "68656A21"
const val LIBRUS_JST_DEMO_PIN = "1290"

const val LIBRUS_SYNERGIA_URL = "https://synergia.librus.pl"
/** https://synergia.librus.pl/loguj/token/TOKEN/przenies */
const val LIBRUS_SYNERGIA_TOKEN_LOGIN_URL = "https://synergia.librus.pl/loguj/token/TOKEN/przenies"

const val LIBRUS_MESSAGES_URL = "https://wiadomosci.librus.pl/module"
const val LIBRUS_SANDBOX_URL = "https://sandbox.librus.pl/index.php?action="

const val LIBRUS_SYNERGIA_HOMEWORK_ATTACHMENT_URL = "https://synergia.librus.pl/homework/downloadFile"
const val LIBRUS_SYNERGIA_MESSAGES_ATTACHMENT_URL = "https://synergia.librus.pl/wiadomosci/pobierz_zalacznik"


val MOBIDZIENNIK_USER_AGENT = SYSTEM_USER_AGENT

const val VULCAN_HEBE_USER_AGENT = "Dart/2.10 (dart:io)"
const val VULCAN_HEBE_APP_NAME = "DzienniczekPlus 2.0"
const val VULCAN_HEBE_APP_VERSION = "22.09.02 (G)"
private const val VULCAN_API_DEVICE_NAME_PREFIX = "Szkolny.eu "
private const val VULCAN_API_DEVICE_NAME_SUFFIX = " - nie usuwać"
val VULCAN_API_DEVICE_NAME by lazy {
    val base = "$VULCAN_API_DEVICE_NAME_PREFIX${Build.MODEL}"
    val baseMaxLength = 50 - VULCAN_API_DEVICE_NAME_SUFFIX.length
    base.take(baseMaxLength) + VULCAN_API_DEVICE_NAME_SUFFIX
}

const val VULCAN_WEB_ENDPOINT_LUCKY_NUMBER = "Start.mvc/GetKidsLuckyNumbers"
const val VULCAN_WEB_ENDPOINT_REGISTER_DEVICE = "RejestracjaUrzadzeniaToken.mvc/Get"
const val VULCAN_HEBE_ENDPOINT_REGISTER_NEW = "api/mobile/register/new"
const val VULCAN_HEBE_ENDPOINT_MAIN = "api/mobile/register/hebe"
const val VULCAN_HEBE_ENDPOINT_PUSH_ALL = "api/mobile/push/all"
const val VULCAN_HEBE_ENDPOINT_TIMETABLE = "api/mobile/schedule"
const val VULCAN_HEBE_ENDPOINT_TIMETABLE_CHANGES = "api/mobile/schedule/changes"
const val VULCAN_HEBE_ENDPOINT_ADDRESSBOOK = "api/mobile/addressbook"
const val VULCAN_HEBE_ENDPOINT_TEACHERS = "api/mobile/teacher"
const val VULCAN_HEBE_ENDPOINT_EXAMS = "api/mobile/exam"
const val VULCAN_HEBE_ENDPOINT_GRADES = "api/mobile/grade"
const val VULCAN_HEBE_ENDPOINT_GRADE_SUMMARY = "api/mobile/grade/summary"
const val VULCAN_HEBE_ENDPOINT_HOMEWORK = "api/mobile/homework"
const val VULCAN_HEBE_ENDPOINT_NOTICES = "api/mobile/note"
const val VULCAN_HEBE_ENDPOINT_ATTENDANCE = "api/mobile/lesson"
const val VULCAN_HEBE_ENDPOINT_MESSAGEBOX = "api/mobile/messagebox"
const val VULCAN_HEBE_ENDPOINT_MESSAGEBOX_ADDRESSBOOK = "api/mobile/messagebox/addressbook"
const val VULCAN_HEBE_ENDPOINT_MESSAGEBOX_MESSAGES = "api/mobile/messagebox/message"
const val VULCAN_HEBE_ENDPOINT_MESSAGEBOX_STATUS = "api/mobile/messagebox/message/status"
const val VULCAN_HEBE_ENDPOINT_MESSAGEBOX_SEND = "api/mobile/messagebox/message"
const val VULCAN_HEBE_ENDPOINT_LUCKY_NUMBER = "api/mobile/school/lucky"

const val PODLASIE_API_VERSION = "1.0.62"
const val PODLASIE_API_URL = "https://cpdklaser.zeto.bialystok.pl/api"
const val PODLASIE_API_USER_ENDPOINT = "/pobierzDaneUcznia"
const val PODLASIE_API_LOGOUT_DEVICES_ENDPOINT = "/wyczyscUrzadzenia"

const val USOS_API_OAUTH_REDIRECT_URL = "szkolny://redirect/usos"

val USOS_API_SCOPES by lazy { listOf(
    "offline_access",
    "studies",
    "grades",
    "events",
) }
