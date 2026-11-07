package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import pl.szczodrzynski.edziennik.data.api.models.Feature
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.db.enums.LoginType

const val ENDPOINT_MOBIDZIENNIK_HTML_GRADES         = 1510
const val ENDPOINT_MOBIDZIENNIK_HTML_TIMETABLE      = 1520
const val ENDPOINT_MOBIDZIENNIK_HTML_AGENDA         = 1530
const val ENDPOINT_MOBIDZIENNIK_HTML_HOMEWORK       = 1540
const val ENDPOINT_MOBIDZIENNIK_HTML_BEHAVIOUR      = 1550
const val ENDPOINT_MOBIDZIENNIK_HTML_ATTENDANCE     = 1565
const val ENDPOINT_MOBIDZIENNIK_HTML_MESSAGES_INBOX = 1570
const val ENDPOINT_MOBIDZIENNIK_HTML_MESSAGES_SENT  = 1575
const val ENDPOINT_MOBIDZIENNIK_HTML_ANNOUNCEMENTS  = 1580

val MobidziennikHtmlFeatures = listOf(
    Feature(LoginType.MOBIDZIENNIK, FeatureType.TIMETABLE, listOf(
        ENDPOINT_MOBIDZIENNIK_HTML_TIMETABLE to LoginMethod.MOBIDZIENNIK_WEB,
    )),
    Feature(LoginType.MOBIDZIENNIK, FeatureType.AGENDA, listOf(
        ENDPOINT_MOBIDZIENNIK_HTML_AGENDA to LoginMethod.MOBIDZIENNIK_WEB,
    )),
    Feature(LoginType.MOBIDZIENNIK, FeatureType.GRADES, listOf(
        ENDPOINT_MOBIDZIENNIK_HTML_GRADES to LoginMethod.MOBIDZIENNIK_WEB,
    )),
    Feature(LoginType.MOBIDZIENNIK, FeatureType.HOMEWORK, listOf(
        ENDPOINT_MOBIDZIENNIK_HTML_HOMEWORK to LoginMethod.MOBIDZIENNIK_WEB,
    )),
    Feature(LoginType.MOBIDZIENNIK, FeatureType.BEHAVIOUR, listOf(
        ENDPOINT_MOBIDZIENNIK_HTML_BEHAVIOUR to LoginMethod.MOBIDZIENNIK_WEB,
    )),
    Feature(LoginType.MOBIDZIENNIK, FeatureType.ATTENDANCE, listOf(
        ENDPOINT_MOBIDZIENNIK_HTML_ATTENDANCE to LoginMethod.MOBIDZIENNIK_WEB,
    )),
    Feature(LoginType.MOBIDZIENNIK, FeatureType.MESSAGES_INBOX, listOf(
        ENDPOINT_MOBIDZIENNIK_HTML_MESSAGES_INBOX to LoginMethod.MOBIDZIENNIK_WEB,
    )),
    Feature(LoginType.MOBIDZIENNIK, FeatureType.MESSAGES_SENT, listOf(
        ENDPOINT_MOBIDZIENNIK_HTML_MESSAGES_SENT to LoginMethod.MOBIDZIENNIK_WEB,
    )),
    Feature(LoginType.MOBIDZIENNIK, FeatureType.ANNOUNCEMENTS, listOf(
        ENDPOINT_MOBIDZIENNIK_HTML_ANNOUNCEMENTS to LoginMethod.MOBIDZIENNIK_WEB,
    )),
)
