package pl.szczodrzynski.edziennik.api.v2.models

import pl.szczodrzynski.edziennik.api.v2.*

val Features = listOf(
        Feature(FEATURE_TIMETABLE, mapOf(
                LOGIN_TYPE_LIBRUS to listOf(
                        LOGIN_MODE_LIBRUS_EMAIL,
                        LOGIN_MODE_LIBRUS_SYNERGIA,
                        LOGIN_MODE_LIBRUS_JST
                )
        ))
)