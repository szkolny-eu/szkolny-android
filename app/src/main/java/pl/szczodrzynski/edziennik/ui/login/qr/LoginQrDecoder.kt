package pl.szczodrzynski.edziennik.ui.login.qr

interface LoginQrDecoder {
    fun decode(value: ByteArray): Map<String, String>
}
