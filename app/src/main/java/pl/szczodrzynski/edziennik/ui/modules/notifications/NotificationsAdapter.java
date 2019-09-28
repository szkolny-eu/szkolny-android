package pl.szczodrzynski.edziennik.ui.modules.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Notification;

import static pl.szczodrzynski.edziennik.utils.Utils.d;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
    private static final String TAG = "NotificationsAdapter";
    private Context context;
    private List<Notification> notificationList;

    //getting the context and product list with constructor
    public NotificationsAdapter(Context mCtx, List<Notification> notificationList) {
        this.context = mCtx;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflating and returning our view holder
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_notifications_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        App app = (App) context.getApplicationContext();

        Notification notification = notificationList.get(position);

        holder.notificationsItemDate.setText(Date.fromMillis(notification.addedDate).getFormattedString());
        holder.notificationsItemText.setText(notification.text);
        holder.notificationsItemTitle.setText(notification.title);
        holder.notificationsItemType.setText(Notification.stringType(context, notification.type));

        holder.notificationsItemCard.setOnClickListener((v -> {
            Intent intent = new Intent("android.intent.action.MAIN");
            notification.fillIntent(intent);

            d(TAG, "notification with item "+notification.redirectFragmentId+" extras "+(intent.getExtras() == null ? "null" : intent.getExtras().toString()));

            //Log.d(TAG, "Got date "+intent.getLongExtra("timetableDate", 0));

            if (notification.profileId != -1 && notification.profileId != app.profile.getId() && context instanceof Activity) {
                Toast.makeText(app, app.getString(R.string.toast_changing_profile), Toast.LENGTH_LONG).show();
            }
            app.sendBroadcast(intent);
        }));

        if (!notification.seen) {
            holder.notificationsItemText.setBackground(context.getResources().getDrawable(R.drawable.bg_rounded_8dp));
            holder.notificationsItemText.getBackground().setColorFilter(new PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY));
        }
        else {
            holder.notificationsItemText.setBackground(null);
        }

    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CardView notificationsItemCard;
        TextView notificationsItemDate;
        TextView notificationsItemText;
        TextView notificationsItemTitle;
        TextView notificationsItemType;

        ViewHolder(View itemView) {
            super(itemView);
            notificationsItemCard = itemView.findViewById(R.id.notificationsItemCard);
            notificationsItemDate = itemView.findViewById(R.id.notificationsItemDate);
            notificationsItemText = itemView.findViewById(R.id.notificationsItemText);
            notificationsItemTitle = itemView.findViewById(R.id.notificationsItemTitle);
            notificationsItemType = itemView.findViewById(R.id.notificationsItemType);
        }
    }
}
