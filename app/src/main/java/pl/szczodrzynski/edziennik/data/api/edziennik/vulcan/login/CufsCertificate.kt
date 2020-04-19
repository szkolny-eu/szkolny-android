/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-17.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login

import pl.droidsonroids.jspoon.annotation.Selector

class CufsCertificate {
        @Selector(value = "EndpointReference Address")
        var targetUrl: String = ""

        @Selector(value = "Lifetime Created")
        var createdDate: String = ""

        @Selector(value = "Lifetime Expires")
        var expiryDate: String = ""

        @Selector(value = "Attribute[AttributeName=UserInstance] AttributeValue")
        var userInstances: List<String> = listOf()
}
