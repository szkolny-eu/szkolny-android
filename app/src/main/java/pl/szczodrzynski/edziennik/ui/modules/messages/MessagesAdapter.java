package pl.szczodrzynski.edziennik.ui.modules.messages;

import android.graphics.Typeface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.databinding.MessagesItemBinding;
import pl.szczodrzynski.edziennik.datamodels.MessageFull;
import pl.szczodrzynski.edziennik.utils.models.Date;

import static pl.szczodrzynski.edziennik.datamodels.Message.TYPE_DRAFT;
import static pl.szczodrzynski.edziennik.datamodels.Message.TYPE_SENT;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
    private App app;
    public List<MessageFull> messageList = new ArrayList<>();
    private AdapterView.OnItemClickListener onItemClickListener;



    public MessagesAdapter(App app, AdapterView.OnItemClickListener onItemClickListener) {
        this.app = app;
        this.onItemClickListener = onItemClickListener;
    }

    void setData(List<MessageFull> messageList) {
        this.messageList = messageList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(DataBindingUtil.inflate(inflater, R.layout.messages_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessagesItemBinding b = holder.b;
        MessageFull message = messageList.get(position);

        b.getRoot().setOnClickListener((v -> {
            onItemClickListener.onItemClick(null, v, position, position);
        }));


        ViewCompat.setTransitionName(b.getRoot(), String.valueOf(message.id));
        /*if (message.type == TYPE_RECEIVED) {
            b.messageSender.setText(message.senderFullName);
        }
        else if (message.type == TYPE_SENT && message.recipients != null && message.recipients.size() > 0) {
            StringBuilder senderText = new StringBuilder();
            boolean first = true;
            for (MessageRecipientFull recipient: message.recipients) {
                if (!first) {
                    senderText.append(", ");
                }
                first = false;
                senderText.append(recipient.fullName);
            }
            b.messageSender.setText(senderText.toString());
        }*/
        b.messageSubject.setText(message.subject);
        b.messageDate.setText(Date.fromMillis(message.addedDate).getFormattedStringShort());
        b.messageAttachmentImage.setVisibility(message.hasAttachments() ? View.VISIBLE : View.GONE);
        try {
            b.messageBody.setText(
                    Html.fromHtml(
                            message.body == null ? "" : message
                                    .body
                                    .substring(0, Math.min(message.body.length(), 200))
                                    .replaceAll("\\[META:[A-z0-9]+;[0-9-]+]", "")
                    )
            );
        }
        catch (Exception e) {
            // TODO ???
        }

        if (message.type == TYPE_SENT || message.type == TYPE_DRAFT || message.seen) {
            b.messageSender.setTextAppearance(b.messageSender.getContext(), R.style.NavView_TextView_Small);
            b.messageSender.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            b.messageSubject.setTextAppearance(b.messageSubject.getContext(), R.style.NavView_TextView_Small);
            b.messageSubject.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            b.messageDate.setTextAppearance(b.messageDate.getContext(), R.style.NavView_TextView_Small);
            b.messageDate.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        }
        else {
            b.messageSender.setTextAppearance(b.messageSender.getContext(), R.style.NavView_TextView);
            b.messageSender.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            b.messageSubject.setTextAppearance(b.messageSubject.getContext(), R.style.NavView_TextView);
            b.messageSubject.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            b.messageDate.setTextAppearance(b.messageDate.getContext(), R.style.NavView_TextView);
            b.messageDate.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }

        /*if (message.type == TYPE_RECEIVED) {
            if (app.appConfig.teacherImages != null && app.appConfig.teacherImages.size() > 0 && app.appConfig.teacherImages.containsKey(message.senderId)) {
                Bitmap profileImage;
                profileImage = BitmapFactory.decodeFile(app.getFilesDir().getAbsolutePath()+"/teacher_"+message.senderId+".jpg");
                profileImage = ThumbnailUtils.extractThumbnail(profileImage, Math.min(profileImage.getWidth(), profileImage.getHeight()), Math.min(profileImage.getWidth(), profileImage.getHeight()));
                RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(app.getResources(), profileImage);
                roundDrawable.setCircular(true);
                b.messageProfileImage.setImageDrawable(roundDrawable);
            }
            else {
                b.messageProfileImage.setImageDrawable(null);
                int color = Colors.stringToMaterialColor(message.senderFullName);
                b.messageProfileBackground.getDrawable().setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
                b.messageProfileName.setTextColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
                if (message.senderFullName == null) {
                    b.messageProfileName.setText("N");
                } else {
                    String[] nameParts = message.senderFullName.split(" ");
                    b.messageProfileName.setText(nameParts[0].toUpperCase().charAt(0) + "" + nameParts[1].toUpperCase().charAt(0));
                }
            }
        }
        else if (message.type == TYPE_SENT && message.recipients != null && message.recipients.size() > 0) {
            MessageRecipientFull recipient = message.recipients.get(0);
            if (message.recipients.size() == 1 && app.appConfig.teacherImages != null && app.appConfig.teacherImages.size() > 0 && app.appConfig.teacherImages.containsKey(recipient.id)) {
                Bitmap profileImage;
                profileImage = BitmapFactory.decodeFile(app.getFilesDir().getAbsolutePath()+"/teacher_"+recipient.id+".jpg");
                profileImage = ThumbnailUtils.extractThumbnail(profileImage, Math.min(profileImage.getWidth(), profileImage.getHeight()), Math.min(profileImage.getWidth(), profileImage.getHeight()));
                RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(app.getResources(), profileImage);
                roundDrawable.setCircular(true);
                b.messageProfileImage.setImageDrawable(roundDrawable);
            }
            else {
                b.messageProfileImage.setImageDrawable(null);
                int color = Colors.stringToMaterialColor(recipient.fullName);
                b.messageProfileBackground.getDrawable().setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
                b.messageProfileName.setTextColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
                if (recipient.fullName == null) {
                    b.messageProfileName.setText("N");
                } else {
                    String[] nameParts = recipient.fullName.split(" ");
                    b.messageProfileName.setText(nameParts[0].toUpperCase().charAt(0) + "" + nameParts[1].toUpperCase().charAt(0));
                }
            }
        }*/

        MessagesUtils.MessageInfo messageInfo = MessagesUtils.getMessageInfo(app, message, 48, 24, 18, 12);
        b.messageProfileBackground.setImageBitmap(messageInfo.profileImage);
        b.messageSender.setText(messageInfo.profileName);


    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        MessagesItemBinding b;

        public ViewHolder(MessagesItemBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
