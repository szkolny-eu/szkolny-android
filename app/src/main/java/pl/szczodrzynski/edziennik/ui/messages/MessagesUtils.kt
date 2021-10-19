package pl.szczodrzynski.edziennik.ui.messages

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Spanned
import androidx.core.graphics.ColorUtils
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.ext.fixName
import pl.szczodrzynski.edziennik.ext.getNameInitials
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
import kotlin.math.roundToInt

object MessagesUtils {
    private fun getPaintCenter(textPaint: Paint): Int {
        return ((textPaint.descent() + textPaint.ascent()) / 2).roundToInt()
    }

    @JvmStatic
    fun getProfileImage(diameterDp: Int, textSizeBigDp: Int, textSizeMediumDp: Int, textSizeSmallDp: Int, count: Int, vararg names: String?): Bitmap {
        val diameter = Utils.dpToPx(diameterDp).toFloat()
        val textSizeBig = Utils.dpToPx(textSizeBigDp).toFloat()
        val textSizeMedium = Utils.dpToPx(textSizeMediumDp).toFloat()
        val textSizeSmall = Utils.dpToPx(textSizeSmallDp).toFloat()
        val bitmap = Bitmap.createBitmap(diameter.toInt(), diameter.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val circlePaint = Paint()
        circlePaint.flags = Paint.ANTI_ALIAS_FLAG
        val textPaint = Paint()
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.flags = Paint.ANTI_ALIAS_FLAG
        val rectF = RectF()
        rectF[0f, 0f, diameter] = diameter
        var name: String?
        var color: Int
        when {
            count == 1 -> {
                name = names[0]
                circlePaint.color = Colors.stringToMaterialColor(name).also { color = it }
                textPaint.color = ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f)
                textPaint.textSize = textSizeBig
                canvas.drawArc(rectF, 0f, 360f, true, circlePaint)
                canvas.drawText(name.getNameInitials(), diameter / 2, diameter / 2 - getPaintCenter(textPaint), textPaint)
            }
            count == 2 -> { // top
                name = names[0]
                circlePaint.color = Colors.stringToMaterialColor(name).also { color = it }
                textPaint.color = ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f)
                textPaint.textSize = textSizeMedium
                canvas.drawArc(rectF, 180f, 180f, true, circlePaint)
                canvas.drawText(name.getNameInitials(), diameter / 2, diameter / 4 - getPaintCenter(textPaint), textPaint)
                // bottom
                name = names[1]
                circlePaint.color = Colors.stringToMaterialColor(name).also { color = it }
                textPaint.color = ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f)
                textPaint.textSize = textSizeMedium
                canvas.drawArc(rectF, 0f, 180f, true, circlePaint)
                canvas.drawText(name.getNameInitials(), diameter / 2, diameter / 4 * 3 - getPaintCenter(textPaint), textPaint)
            }
            count == 3 -> { // upper left
                name = names[0]
                circlePaint.color = Colors.stringToMaterialColor(name).also { color = it }
                textPaint.color = ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f)
                textPaint.textSize = textSizeSmall
                canvas.drawArc(rectF, 180f, 90f, true, circlePaint)
                canvas.drawText(name.getNameInitials(), diameter / 4, diameter / 4 - getPaintCenter(textPaint) + diameter / 32, textPaint)
                // upper right
                name = names[1]
                circlePaint.color = Colors.stringToMaterialColor(name).also { color = it }
                textPaint.color = ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f)
                textPaint.textSize = textSizeSmall
                canvas.drawArc(rectF, 270f, 90f, true, circlePaint)
                canvas.drawText(name.getNameInitials(), diameter / 4 * 3, diameter / 4 - getPaintCenter(textPaint) + diameter / 32, textPaint)
                // bottom
                name = names[2]
                circlePaint.color = Colors.stringToMaterialColor(name).also { color = it }
                textPaint.color = ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f)
                textPaint.textSize = textSizeMedium
                canvas.drawArc(rectF, 0f, 180f, true, circlePaint)
                canvas.drawText(name.getNameInitials(), diameter / 2, diameter / 4 * 3 - getPaintCenter(textPaint), textPaint)
            }
            count >= 4 -> { // upper left
                name = names[0]
                circlePaint.color = Colors.stringToMaterialColor(name).also { color = it }
                textPaint.color = ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f)
                textPaint.textSize = textSizeSmall
                canvas.drawArc(rectF, 180f, 90f, true, circlePaint)
                canvas.drawText(name.getNameInitials(), diameter / 4, diameter / 4 - getPaintCenter(textPaint) + diameter / 32, textPaint)
                // upper right
                name = names[1]
                circlePaint.color = Colors.stringToMaterialColor(name).also { color = it }
                textPaint.color = ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f)
                textPaint.textSize = textSizeSmall
                canvas.drawArc(rectF, 270f, 90f, true, circlePaint)
                canvas.drawText(name.getNameInitials(), diameter / 4 * 3, diameter / 4 - getPaintCenter(textPaint) + diameter / 32, textPaint)
                // bottom left
                name = names[2]
                circlePaint.color = Colors.stringToMaterialColor(name).also { color = it }
                textPaint.color = ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f)
                textPaint.textSize = textSizeSmall
                canvas.drawArc(rectF, 90f, 90f, true, circlePaint)
                canvas.drawText(name.getNameInitials(), diameter / 4, diameter / 4 * 3 - getPaintCenter(textPaint) - diameter / 32, textPaint)
                // bottom right
                if (count == 4) name = names[3]
                if (count > 4) name = "..."
                circlePaint.color = Colors.stringToMaterialColor(name).also { color = it }
                textPaint.color = ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f)
                textPaint.textSize = textSizeSmall
                canvas.drawArc(rectF, 0f, 90f, true, circlePaint)
                canvas.drawText(if (count > 4) "+" + (count - 3) else name.getNameInitials(), diameter / 4 * 3, diameter / 4 * 3 - getPaintCenter(textPaint) - diameter / 32, textPaint)
            }
        }
        return bitmap
    }

    fun getMessageInfo(app: App, message: MessageFull, diameterDp: Int, textSizeBigDp: Int, textSizeMediumDp: Int, textSizeSmallDp: Int): MessageInfo {
        var profileImage: Bitmap? = null
        var profileName: String? = null
        if (message.isReceived || message.isDeleted) {
            profileName = message.senderName?.fixName()
            profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 1, profileName)
        } else if (message.isSent || message.isDraft && message.recipients != null) {
            when (val count = message.recipients?.size ?: 0) {
                0 -> {
                    profileName = app.getString(R.string.messages_draft_title)
                    profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 1, "?")
                }
                1 -> {
                    val recipient = message.recipients!![0]
                    profileName = recipient.fullName
                    profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 1, recipient.fullName)
                }
                2 -> {
                    val recipient1 = message.recipients!![0]
                    val recipient2 = message.recipients!![1]
                    profileName = recipient1.fullName + ", " + recipient2.fullName
                    profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 2, recipient1.fullName, recipient2.fullName)
                }
                3 -> {
                    val recipient1 = message.recipients!![0]
                    val recipient2 = message.recipients!![1]
                    val recipient3 = message.recipients!![2]
                    profileName = recipient1.fullName + ", " + recipient2.fullName + ", " + recipient3.fullName
                    profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 3, recipient1.fullName, recipient2.fullName, recipient3.fullName)
                }
                4 -> {
                    val recipient1 = message.recipients!![0]
                    val recipient2 = message.recipients!![1]
                    val recipient3 = message.recipients!![2]
                    val recipient4 = message.recipients!![3]
                    profileName = recipient1.fullName + ", " + recipient2.fullName + ", " + recipient3.fullName + ", " + recipient4.fullName
                    profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 4, recipient1.fullName, recipient2.fullName, recipient3.fullName, recipient4.fullName)
                }
                else -> {
                    val recipient1 = message.recipients!![0]
                    val recipient2 = message.recipients!![1]
                    val recipient3 = message.recipients!![2]
                    val senderText = StringBuilder()
                    var first = true
                    for (recipient in message.recipients!!) {
                        if (!first) {
                            senderText.append(", ")
                        }
                        first = false
                        senderText.append(recipient.fullName)
                    }
                    profileName = senderText.toString()
                    profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, count, recipient1.fullName, recipient2.fullName, recipient3.fullName)
                }
            }
        }
        return MessageInfo(profileImage, profileName)
    }

    class MessageInfo(var profileImage: Bitmap?, var profileName: String?)

    @JvmStatic
    fun htmlToSpannable(context: Context, html: String): Spanned {
        return BetterHtml.fromHtml(context, html)
    }
}
