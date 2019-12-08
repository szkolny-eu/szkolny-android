package pl.szczodrzynski.edziennik.ui.modules.announcements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.announcements.AnnouncementFull;
import pl.szczodrzynski.edziennik.databinding.RowAnnouncementsItemBinding;
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesUtils;

public class AnnouncementsAdapter extends RecyclerView.Adapter<AnnouncementsAdapter.ViewHolder> {

    private Context context;
    public List<AnnouncementFull> announcementList;
    public OnAnnouncementClickListener onClick;

    public interface OnAnnouncementClickListener {
        void onClick(View v, AnnouncementFull announcement);
    }

    public AnnouncementsAdapter(Context context, List<AnnouncementFull> announcementList, OnAnnouncementClickListener onClick) {
        //setHasStableIds(true);

        this.context = context;
        this.announcementList = announcementList;
        this.onClick = onClick;
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.row_announcements_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int groupPosition) {
        AnnouncementFull item = announcementList.get(groupPosition);
        RowAnnouncementsItemBinding b = holder.b;

        b.announcementsItem.setOnClickListener((v -> {
            if (onClick != null) {
                onClick.onClick(v, item);
            }
        }));
        b.announcementsItemSender.setText(item.teacherFullName);
        b.announcementsItemTitle.setText(item.subject);
        b.announcementsItemText.setText(item.text);

        if (item.endDate == null) {
            b.announcementsItemDate.setText(item.startDate.getFormattedString());
        } else {
            b.announcementsItemDate.setText(context.getString(R.string.date_relative_format, item.startDate.getFormattedStringShort(), item.endDate.getFormattedStringShort()));
        }

        if (!item.seen) {
            b.announcementsItemTitle.setBackground(context.getResources().getDrawable(R.drawable.bg_rounded_8dp));
            b.announcementsItemTitle.getBackground().setColorFilter(new PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY));
            b.announcementsItemSender.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            b.announcementsItemTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        } else {
            b.announcementsItemSender.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            b.announcementsItemTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            b.announcementsItemTitle.setBackground(null);
        }

        Bitmap profileImage = MessagesUtils.getProfileImage(48, 24, 18, 12, 1, item.teacherFullName);
        b.announcementsItemImage.setImageBitmap(profileImage);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        RowAnnouncementsItemBinding b;

        ViewHolder(RowAnnouncementsItemBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
