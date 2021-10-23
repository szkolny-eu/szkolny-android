package pl.szczodrzynski.edziennik.ui.login.qr

interface LoginQrDecoder {

    fun decode(value: String): Map<String, String>?
    fun focusFieldName(): String?
}
