package pl.szczodrzynski.edziennik.ui.modules.messages;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.core.graphics.ColorUtils;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipientFull;
import pl.szczodrzynski.edziennik.utils.Colors;
import pl.szczodrzynski.edziennik.utils.Utils;

import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_DELETED;
import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_DRAFT;
import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_RECEIVED;
import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_SENT;

public class MessagesUtils {
    public static class MessageInfo {
        public Bitmap profileImage;
        public String profileName;

        public MessageInfo(Bitmap profileImage, String profileName) {
            this.profileImage = profileImage;
            this.profileName = profileName;
        }
    }

    private static String getInitials(String name) {
        if (name == null || name.isEmpty())
            return "";
        name = name.toUpperCase();
        String[] nameParts = name.split(" ");
        return nameParts.length <= 1 ?
                (name.length() == 0 ? "?" : name.charAt(0))+"" :
                (nameParts[0].length() == 0 ? "?" : nameParts[0].charAt(0))
                + "" +
                (nameParts[1].length() == 0 ? "?" : nameParts[1].charAt(0));
    }

    private static int getPaintCenter(Paint textPaint) {
        return Math.round((textPaint.descent() + textPaint.ascent()) / 2);
    }

    public static Bitmap getProfileImage(int diameterDp, int textSizeBigDp, int textSizeMediumDp, int textSizeSmallDp, int count, String ... names) {
        float diameter = Utils.dpToPx(diameterDp);
        float textSizeBig = Utils.dpToPx(textSizeBigDp);
        float textSizeMedium = Utils.dpToPx(textSizeMediumDp);
        float textSizeSmall = Utils.dpToPx(textSizeSmallDp);

        Bitmap bitmap = Bitmap.createBitmap((int) diameter, (int) diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circlePaint = new Paint();
        circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        Paint textPaint = new Paint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        RectF rectF = new RectF();
        rectF.set(0, 0, diameter, diameter);

        String name;
        int color;

        if (count == 1) {
            name = names[0];
            circlePaint.setColor(color = Colors.stringToMaterialColor(name));
            textPaint.setColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
            textPaint.setTextSize(textSizeBig);
            canvas.drawArc(rectF, 0, 360, true, circlePaint);
            canvas.drawText(getInitials(name), diameter/2, diameter/2 - getPaintCenter(textPaint), textPaint);
        }
        else if (count == 2) {
            // top
            name = names[0];
            circlePaint.setColor(color = Colors.stringToMaterialColor(name));
            textPaint.setColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
            textPaint.setTextSize(textSizeMedium);
            canvas.drawArc(rectF, 180, 180, true, circlePaint);
            canvas.drawText(getInitials(name), diameter/2, diameter/4 - getPaintCenter(textPaint), textPaint);

            // bottom
            name = names[1];
            circlePaint.setColor(color = Colors.stringToMaterialColor(name));
            textPaint.setColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
            textPaint.setTextSize(textSizeMedium);
            canvas.drawArc(rectF, 0, 180, true, circlePaint);
            canvas.drawText(getInitials(name), diameter/2, diameter/4*3 - getPaintCenter(textPaint), textPaint);
        }
        else if (count == 3) {
            // upper left
            name = names[0];
            circlePaint.setColor(color = Colors.stringToMaterialColor(name));
            textPaint.setColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
            textPaint.setTextSize(textSizeSmall);
            canvas.drawArc(rectF, 180, 90, true, circlePaint);
            canvas.drawText(getInitials(name), diameter/4, diameter/4 - getPaintCenter(textPaint) + diameter/32, textPaint);

            // upper right
            name = names[1];
            circlePaint.setColor(color = Colors.stringToMaterialColor(name));
            textPaint.setColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
            textPaint.setTextSize(textSizeSmall);
            canvas.drawArc(rectF, 270, 90, true, circlePaint);
            canvas.drawText(getInitials(name), diameter/4*3, diameter/4 - getPaintCenter(textPaint) + diameter/32, textPaint);

            // bottom
            name = names[2];
            circlePaint.setColor(color = Colors.stringToMaterialColor(name));
            textPaint.setColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
            textPaint.setTextSize(textSizeMedium);
            canvas.drawArc(rectF, 0, 180, true, circlePaint);
            canvas.drawText(getInitials(name), diameter/2, diameter/4*3 - getPaintCenter(textPaint), textPaint);
        }
        else if (count >= 4) {
            // upper left
            name = names[0];
            circlePaint.setColor(color = Colors.stringToMaterialColor(name));
            textPaint.setColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
            textPaint.setTextSize(textSizeSmall);
            canvas.drawArc(rectF, 180, 90, true, circlePaint);
            canvas.drawText(getInitials(name), diameter/4, diameter/4 - getPaintCenter(textPaint) + diameter/32, textPaint);

            // upper right
            name = names[1];
            circlePaint.setColor(color = Colors.stringToMaterialColor(name));
            textPaint.setColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
            textPaint.setTextSize(textSizeSmall);
            canvas.drawArc(rectF, 270, 90, true, circlePaint);
            canvas.drawText(getInitials(name), diameter/4*3, diameter/4 - getPaintCenter(textPaint) + diameter/32, textPaint);

            // bottom left
            name = names[2];
            circlePaint.setColor(color = Colors.stringToMaterialColor(name));
            textPaint.setColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
            textPaint.setTextSize(textSizeSmall);
            canvas.drawArc(rectF, 90, 90, true, circlePaint);
            canvas.drawText(getInitials(name), diameter/4, diameter/4*3 - getPaintCenter(textPaint) - diameter/32, textPaint);

            // bottom right
            if (count == 4)
                name = names[3];
            if (count > 4)
                name = "...";
            circlePaint.setColor(color = Colors.stringToMaterialColor(name));
            textPaint.setColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
            textPaint.setTextSize(textSizeSmall);
            canvas.drawArc(rectF, 0, 90, true, circlePaint);
            canvas.drawText(count > 4 ? "+"+(count-3) : getInitials(name), diameter/4*3, diameter/4*3 - getPaintCenter(textPaint) - diameter/32, textPaint);
        }

        return bitmap;
    }

    public static MessageInfo getMessageInfo(App app, MessageFull message, int diameterDp, int textSizeBigDp, int textSizeMediumDp, int textSizeSmallDp) {
        Bitmap profileImage = null;
        String profileName = null;
        if (message.type == TYPE_RECEIVED || message.type == TYPE_DELETED) {
            profileName = message.senderFullName;
            profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 1, message.senderFullName);
        }
        else if (message.type == TYPE_SENT || message.type == TYPE_DRAFT && message.recipients != null) {
            int count = message.recipients == null ? 0 : message.recipients.size();
            if (count == 0) {
                profileName = app.getString(R.string.messages_draft_title);
                profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 1, "?");
            }
            else if (count == 1) {
                MessageRecipientFull recipient = message.recipients.get(0);
                profileName = recipient.fullName;
                profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 1, recipient.fullName);
            }
            else if (count == 2) {
                MessageRecipientFull recipient1 = message.recipients.get(0);
                MessageRecipientFull recipient2 = message.recipients.get(1);
                profileName = recipient1.fullName+", "+recipient2.fullName;
                profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 2, recipient1.fullName, recipient2.fullName);
            }
            else if (count == 3) {
                MessageRecipientFull recipient1 = message.recipients.get(0);
                MessageRecipientFull recipient2 = message.recipients.get(1);
                MessageRecipientFull recipient3 = message.recipients.get(2);
                profileName = recipient1.fullName+", "+recipient2.fullName+", "+recipient3.fullName;
                profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 3, recipient1.fullName, recipient2.fullName, recipient3.fullName);
            }
            else if (count == 4) {
                MessageRecipientFull recipient1 = message.recipients.get(0);
                MessageRecipientFull recipient2 = message.recipients.get(1);
                MessageRecipientFull recipient3 = message.recipients.get(2);
                MessageRecipientFull recipient4 = message.recipients.get(3);
                profileName = recipient1.fullName+", "+recipient2.fullName+", "+recipient3.fullName+", "+recipient4.fullName;
                profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, 4, recipient1.fullName, recipient2.fullName, recipient3.fullName, recipient4.fullName);
            }
            else {
                MessageRecipientFull recipient1 = message.recipients.get(0);
                MessageRecipientFull recipient2 = message.recipients.get(1);
                MessageRecipientFull recipient3 = message.recipients.get(2);

                StringBuilder senderText = new StringBuilder();
                boolean first = true;
                for (MessageRecipientFull recipient: message.recipients) {
                    if (!first) {
                        senderText.append(", ");
                    }
                    first = false;
                    senderText.append(recipient.fullName);
                }
                profileName = senderText.toString();

                profileImage = getProfileImage(diameterDp, textSizeBigDp, textSizeMediumDp, textSizeSmallDp, count, recipient1.fullName, recipient2.fullName, recipient3.fullName);
            }
        }
        return new MessageInfo(profileImage, profileName);
    }
}
