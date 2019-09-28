package pl.szczodrzynski.edziennik.ui.dialogs.event;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mikepenz.iconics.view.IconicsImageView;
import com.mikepenz.iconics.view.IconicsTextView;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.datamodels.Event;
import pl.szczodrzynski.edziennik.datamodels.EventFull;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.Utils;

import static pl.szczodrzynski.edziennik.datamodels.Event.TYPE_HOMEWORK;
import static pl.szczodrzynski.edziennik.utils.Utils.bs;
import static pl.szczodrzynski.edziennik.utils.Utils.d;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {
    private static final String TAG = "EventListAdapter";
    private Context context;
    private List<EventFull> examList;
    private EventListDialog parentDialog;

    //getting the context and product list with constructor
    public EventListAdapter(Context mCtx, List<EventFull> examList, EventListDialog parentDialog) {
        this.context = mCtx;
        this.examList = examList;
        this.parentDialog = parentDialog;
    }

    @NonNull
    @Override
    public EventListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflating and returning our view holder
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_dialog_event_list_item, parent, false);
        return new EventListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventListAdapter.ViewHolder holder, int position) {
        App app = (App) context.getApplicationContext();

        EventFull event = examList.get(position);

        if (event.type == TYPE_HOMEWORK) {
            holder.eventListItemRoot.getBackground().setColorFilter(new PorterDuffColorFilter(0xffffffff, PorterDuff.Mode.CLEAR));
        }
        else {
            holder.eventListItemRoot.getBackground().setColorFilter(new PorterDuffColorFilter(event.getColor(), PorterDuff.Mode.MULTIPLY));
        }

        holder.eventListItemStartTime.setText(event.startTime == null ? app.getString(R.string.event_all_day) : event.startTime.getStringHM());
        //holder.examListItemEndTime.setText(event.endTime.getStringHM());
        holder.eventListItemTeamName.setText(Utils.bs(event.teamName));
        holder.eventListItemTeacherName.setText(bs(null, event.teacherFullName, "\n") + bs(event.subjectLongName));
        holder.eventListItemAddedDate.setText(Date.fromMillis(event.addedDate).getFormattedStringShort());
        holder.eventListItemType.setText(event.typeName);
        holder.eventListItemTopic.setText(event.topic);
        holder.eventListItemEdit.setVisibility((event.addedManually ? View.VISIBLE : View.GONE));
        holder.eventListItemHomework.setVisibility((event.type == Event.TYPE_HOMEWORK ? View.VISIBLE : View.GONE));
        holder.eventListItemEdit.setOnClickListener(v -> {
            if (parentDialog != null) {
                parentDialog.callDismissListener = false;
                parentDialog.dialog.dismiss();
            }
            d(TAG, "Event "+event);
            if (event.type == Event.TYPE_HOMEWORK) {
                new EventManualDialog(context, event.profileId)
                        .withDismissListener(parentDialog::performDismiss)
                        .show(app, event, null, null, EventManualDialog.DIALOG_HOMEWORK);
            }
            else {
                new EventManualDialog(context, event.profileId)
                        .withDismissListener(parentDialog::performDismiss)
                        .show(app, event, null, null, EventManualDialog.DIALOG_EVENT);
            }
        });

        if (event.sharedBy == null) {
            holder.eventListItemSharedBy.setVisibility(View.GONE);
        }
        else if (event.sharedByName != null) {
            holder.eventListItemSharedBy.setText(app.getString(R.string.event_shared_by_format, (event.sharedBy.equals("self") ? app.getString(R.string.event_shared_by_self) : event.sharedByName)));
        }
    }

    @Override
    public int getItemCount() {
        return examList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout eventListItemRoot;
        TextView eventListItemStartTime;
        TextView eventListItemTeacherName;
        TextView eventListItemType;
        TextView eventListItemTopic;
        TextView eventListItemAddedDate;
        TextView eventListItemTeamName;
        IconicsImageView eventListItemEdit;
        IconicsImageView eventListItemHomework;
        IconicsTextView eventListItemSharedBy;


        ViewHolder(View itemView) {
            super(itemView);
            eventListItemRoot = itemView.findViewById(R.id.eventListItemRoot);
            eventListItemStartTime = itemView.findViewById(R.id.eventListItemStartTime);
            eventListItemTeacherName = itemView.findViewById(R.id.eventListItemTeacherName);
            eventListItemType = itemView.findViewById(R.id.eventListItemType);
            eventListItemTopic = itemView.findViewById(R.id.eventListItemTopic);
            eventListItemAddedDate = itemView.findViewById(R.id.eventListItemAddedDate);
            eventListItemTeamName = itemView.findViewById(R.id.eventListItemTeamName);
            eventListItemEdit = itemView.findViewById(R.id.eventListItemEdit);
            eventListItemHomework = itemView.findViewById(R.id.eventListItemHomework);
            eventListItemSharedBy = itemView.findViewById(R.id.eventListItemSharedBy);
        }
    }
}
