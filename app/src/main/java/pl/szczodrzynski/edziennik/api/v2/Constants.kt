/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-19.
 */

package pl.szczodrzynski.edziennik.api.v2

internal const val FEATURE_ANY = -1
const val FEATURE_ALL = 0
const val FEATURE_TIMETABLE = 1
const val FEATURE_AGENDA = 2
const val FEATURE_GRADES = 3
const val FEATURE_HOMEWORKS = 4
const val FEATURE_NOTICES = 5
const val FEATURE_ATTENDANCES = 6
const val FEATURE_MESSAGES_INBOX = 7
const val FEATURE_MESSAGES_OUTBOX = 8
const val FEATURE_ANNOUNCEMENTS = 9

const val LOGIN_TYPE_MOBIDZIENNIK = 1
const val LOGIN_TYPE_LIBRUS = 2
const val LOGIN_TYPE_IUCZNIOWIE = 3
const val LOGIN_TYPE_VULCAN = 4
const val LOGIN_TYPE_DEMO = 20

// LOGIN MODES
const val LOGIN_MODE_LIBRUS_EMAIL = 0
const val LOGIN_MODE_LIBRUS_SYNERGIA = 1
const val LOGIN_MODE_LIBRUS_JST = 2
const val LOGIN_MODE_MOBIDZIENNIK_WEB = 0
const val LOGIN_MODE_IDZIENNIK_WEB = 0
const val LOGIN_MODE_VULCAN_WEB = 0

// LOGIN METHODS
const val LOGIN_METHOD_NOT_NEEDED = -1
const val LOGIN_METHOD_LIBRUS_PORTAL = 0
const val LOGIN_METHOD_LIBRUS_API = 1
const val LOGIN_METHOD_LIBRUS_SYNERGIA = 2
const val LOGIN_METHOD_LIBRUS_MESSAGES = 3
const val LOGIN_METHOD_MOBIDZIENNIK_API = 0
const val LOGIN_METHOD_IDZIENNIK_WEB = 0
const val LOGIN_METHOD_IDZIENNIK_API = 1
const val LOGIN_METHOD_VULCAN_WEB = 0
const val LOGIN_METHOD_VULCAN_API = 1

const val LIBRUS_USER_AGENT = "Dalvik/2.1.0 Android LibrusMobileApp"
const val LIBRUS_CLIENT_ID = "wmSyUMo8llDAs4y9tJVYY92oyZ6h4lAt7KCuy0Gv"
const val LIBRUS_REDIRECT_URL = "http://localhost/bar"
const val LIBRUS_AUTHORIZE_URL = "https://portal.librus.pl/oauth2/authorize?client_id=$LIBRUS_CLIENT_ID&redirect_uri=$LIBRUS_REDIRECT_URL&response_type=code"
const val LIBRUS_LOGIN_URL = "https://portal.librus.pl/rodzina/login/action"
const val LIBRUS_TOKEN_URL = "https://portal.librus.pl/oauth2/access_token"

const val LIBRUS_ACCOUNT_URL = "https://portal.librus.pl/api/v2/SynergiaAccounts/fresh/" // + login
const val LIBRUS_ACCOUNTS_URL = "https://portal.librus.pl/api/v2/SynergiaAccounts"
