package pl.szczodrzynski.edziennik.ui.modules.homework;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.full.EventFull;
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog;
import pl.szczodrzynski.edziennik.utils.models.Date;

import static pl.szczodrzynski.edziennik.utils.Utils.bs;

public class HomeworkAdapter extends RecyclerView.Adapter<HomeworkAdapter.ViewHolder> {
    private Context context;
    private List<EventFull> homeworkList;

    //getting the context and product list with constructor
    public HomeworkAdapter(Context mCtx, List<EventFull> homeworkList) {
        this.context = mCtx;
        this.homeworkList = homeworkList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflating and returning our view holder
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_homework_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        App app = (App) context.getApplicationContext();

        EventFull homework = homeworkList.get(position);

        int diffDays = Date.diffDays(homework.getDate(), Date.getToday());

        holder.homeworkItemHomeworkDate.setText(app.getString(R.string.date_relative_format, homework.getDate().getFormattedString(), Date.dayDiffString(context, diffDays)));
        holder.homeworkItemTopic.setText(homework.getTopic());
        holder.homeworkItemSubjectTeacher.setText(context.getString(R.string.homework_subject_teacher_format, bs(homework.getSubjectLongName()), bs(homework.getTeacherFullName())));
        holder.homeworkItemTeamDate.setText(context.getString(R.string.homework_team_date_format, bs(homework.getTeamName()), Date.fromMillis(homework.getAddedDate()).getFormattedStringShort()));

        if (!homework.getSeen()) {
            holder.homeworkItemTopic.setBackground(context.getResources().getDrawable(R.drawable.bg_rounded_8dp));
            holder.homeworkItemTopic.getBackground().setColorFilter(new PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY));
            homework.setSeen(true);
            AsyncTask.execute(() -> {
                App.db.metadataDao().setSeen(App.Companion.getProfileId(), homework, true);
            });
        }
        else {
            holder.homeworkItemTopic.setBackground(null);
        }

        holder.homeworkItemEdit.setVisibility((homework.getAddedManually() ? View.VISIBLE : View.GONE));
        holder.homeworkItemEdit.setOnClickListener(v -> {
            new EventManualDialog(
                    (MainActivity) context,
                    homework.getProfileId(),
                    null,
                    null,
                    null,
                    null,
                    homework,
                    null,
                    null);
        });

        if (homework.getSharedBy() == null) {
            holder.homeworkItemSharedBy.setVisibility(View.GONE);
        }
        else if (homework.getSharedByName() != null) {
            holder.homeworkItemSharedBy.setText(app.getString(R.string.event_shared_by_format, (homework.getSharedBy().equals("self") ? app.getString(R.string.event_shared_by_self) : homework.getSharedByName())));
        }
    }

    @Override
    public int getItemCount() {
        return homeworkList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CardView homeworkItemCard;
        TextView homeworkItemTopic;
        TextView homeworkItemHomeworkDate;
        TextView homeworkItemSharedBy;
        TextView homeworkItemSubjectTeacher;
        TextView homeworkItemTeamDate;
        Button homeworkItemEdit;

        ViewHolder(View itemView) {
            super(itemView);
            homeworkItemCard = itemView.findViewById(R.id.homeworkItemCard);
            homeworkItemTopic = itemView.findViewById(R.id.homeworkItemTopic);
            homeworkItemHomeworkDate = itemView.findViewById(R.id.homeworkItemHomeworkDate);
            homeworkItemSharedBy = itemView.findViewById(R.id.homeworkItemSharedBy);
            homeworkItemSubjectTeacher = itemView.findViewById(R.id.homeworkItemSubjectTeacher);
            homeworkItemTeamDate = itemView.findViewById(R.id.homeworkItemTeamDate);
            homeworkItemEdit = itemView.findViewById(R.id.homeworkItemEdit);
        }
    }
}
