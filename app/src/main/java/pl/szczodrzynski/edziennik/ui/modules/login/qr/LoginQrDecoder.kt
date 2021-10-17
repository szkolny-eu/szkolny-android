package pl.szczodrzynski.edziennik.ui.modules.login.qr

interface LoginQrDecoder {
    fun decode(value: ByteArray): Map<String, String>
}