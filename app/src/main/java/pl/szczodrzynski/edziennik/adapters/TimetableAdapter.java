package pl.szczodrzynski.edziennik.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.datamodels.EventFull;
import pl.szczodrzynski.edziennik.datamodels.LessonChange;
import pl.szczodrzynski.edziennik.datamodels.LessonFull;
import pl.szczodrzynski.edziennik.dialogs.EventListDialog;
import pl.szczodrzynski.edziennik.models.Date;
import pl.szczodrzynski.edziennik.utils.SpannableHtmlTagHandler;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.Utils;

import static pl.szczodrzynski.edziennik.datamodels.Event.TYPE_HOMEWORK;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.ViewHolder> {
    private static final String TAG = "TimetableAdapter";
    private Context context;
    private Date lessonDate;
    private List<LessonFull> lessonList;
    private List<EventFull> eventList;
    public boolean setAsRead = false;

    //getting the context and product list with constructor
    public TimetableAdapter(Context mCtx, Date lessonDate, List<LessonFull> lessonList, List<EventFull> eventList) {
        this.context = mCtx;
        this.lessonDate = lessonDate;
        this.lessonList = lessonList;
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflating and returning our view holder
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_timetable_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        App app = (App) context.getApplicationContext();

        LessonFull lesson = lessonList.get(position);
        holder.timetableItemStartTime.setText(lesson.startTime.getStringHM());
        holder.timetableItemEndTime.setText(lesson.endTime.getStringHM());
        holder.timetableItemSubjectName.setText(lesson.subjectLongName);
        holder.timetableItemClassroomName.setText(lesson.getClassroomName());
        holder.timetableItemTeamName.setText(lesson.getTeamName());
        holder.timetableItemTeacherName.setText(lesson.getTeacherFullName());

        holder.timetableItemCard.setCardBackgroundColor(Utils.getAttr(context, R.attr.colorSurface));

        if (lesson.changeId != 0)
        {
            if (!lesson.seen) {
                holder.timetableItemSubjectName.setBackground(context.getResources().getDrawable(R.drawable.bg_rounded_4dp));
                holder.timetableItemSubjectName.getBackground().setColorFilter(new PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY));
                AsyncTask.execute(() -> app.db.metadataDao().setSeen(App.profileId, lesson, true));
            }
            if (lesson.changeType == LessonChange.TYPE_CANCELLED)
            {
                holder.timetableItemSubjectName.setTypeface(null, Typeface.NORMAL);
                holder.timetableItemSubjectChange.setVisibility(View.GONE);

                holder.timetableItemSubjectName.setText(Html.fromHtml("<del>"+lesson.subjectLongName+"</del>", null, new SpannableHtmlTagHandler()));
            }
            else if (lesson.changeType == LessonChange.TYPE_CHANGE)
            {
                holder.timetableItemSubjectName.setTypeface(null, Typeface.BOLD_ITALIC);
                if (lesson.changedSubjectLongName()) {
                    holder.timetableItemSubjectChange.setText(lesson.subjectLongName);
                    holder.timetableItemSubjectName.setText(lesson.changeSubjectLongName);
                    holder.timetableItemSubjectChange.setVisibility(View.VISIBLE);

                    holder.timetableItemSubjectChange.setText(Html.fromHtml("<del>"+lesson.subjectLongName+"</del>", null, new SpannableHtmlTagHandler()));
                }
                else {
                    holder.timetableItemSubjectChange.setVisibility(View.GONE);
                }
                holder.timetableItemTeacherName.setText(lesson.getTeacherFullName(true));
                holder.timetableItemTeamName.setText(lesson.getTeamName(true));
                holder.timetableItemClassroomName.setText(lesson.getClassroomName(true));
            }
            else if (lesson.changeType == LessonChange.TYPE_ADDED)
            {

            }


            holder.timetableItemLayout.getBackground().setColorFilter(new PorterDuffColorFilter(0xffffffff, PorterDuff.Mode.CLEAR));

            if (lesson.changeType == LessonChange.TYPE_CANCELLED) {
                holder.timetableItemCard.setCardBackgroundColor(Themes.INSTANCE.isDark() ? 0x60000000 : 0xffeeeeee);
            }
            else if (lesson.changeType == LessonChange.TYPE_CHANGE) {
                holder.timetableItemLayout.getBackground().setColorFilter(new PorterDuffColorFilter(Utils.getAttr(context, R.attr.colorPrimary), PorterDuff.Mode.SRC_ATOP));//.setBackgroundColor(App.getAttr(context, R.attr.cardBackgroundHighlight));
            }
        }
        else
        {
            holder.timetableItemLayout.getBackground().setColorFilter(new PorterDuffColorFilter(0xffffffff, PorterDuff.Mode.CLEAR));
            holder.timetableItemSubjectName.setBackground(null);
            holder.timetableItemSubjectName.setTypeface(null, Typeface.NORMAL);
            holder.timetableItemSubjectChange.setVisibility(View.GONE);
        }

        int eventCount = 0;

        for (EventFull event: eventList) {
            if (event.eventDate.getValue() == lessonDate.getValue()
                    && event.startTime != null
                    && event.startTime.getValue() == lesson.startTime.getValue()) {
                eventCount++;
                if (eventCount == 1) {
                    holder.timetableItemEvent1.setVisibility(View.VISIBLE);
                    if (event.type == TYPE_HOMEWORK)
                        holder.timetableItemEvent1.setBackground(new IconicsDrawable(context).color(IconicsColor.colorRes(R.color.md_red_500)).size(IconicsSize.dp(10)).icon(CommunityMaterial.Icon2.cmd_home));
                    else
                        holder.timetableItemEvent1.setBackgroundColor(event.getColor());
                }
                else if (eventCount == 2) {
                    holder.timetableItemEvent2.setVisibility(View.VISIBLE);
                    if (event.type == TYPE_HOMEWORK)
                        holder.timetableItemEvent2.setBackground(new IconicsDrawable(context).color(IconicsColor.colorRes(R.color.md_red_500)).size(IconicsSize.dp(10)).icon(CommunityMaterial.Icon2.cmd_home));
                    else
                        holder.timetableItemEvent2.setBackgroundColor(event.getColor());
                }
                else if (eventCount == 3) {
                    holder.timetableItemEvent3.setVisibility(View.VISIBLE);
                    if (event.type == TYPE_HOMEWORK)
                        holder.timetableItemEvent3.setBackground(new IconicsDrawable(context).color(IconicsColor.colorRes(R.color.md_red_500)).size(IconicsSize.dp(10)).icon(CommunityMaterial.Icon2.cmd_home));
                    else
                        holder.timetableItemEvent3.setBackgroundColor(event.getColor());
                }
            }
        }

        holder.timetableItemCard.setOnClickListener(v -> new EventListDialog(context).show(app, lessonDate, lesson.startTime));
    }

    @Override
    public int getItemCount() {
        return lessonList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CardView timetableItemCard;
        ConstraintLayout timetableItemLayout;
        TextView timetableItemStartTime;
        TextView timetableItemClassroomName;
        TextView timetableItemTeamName;
        TextView timetableItemSubjectChange;
        TextView timetableItemSubjectName;
        TextView timetableItemTeacherName;
        TextView timetableItemEndTime;
        View timetableItemEvent1;
        View timetableItemEvent2;
        View timetableItemEvent3;

        ViewHolder(View itemView) {
            super(itemView);
            timetableItemCard = itemView.findViewById(R.id.timetableItemCard);
            timetableItemLayout = itemView.findViewById(R.id.timetableItemLayout);
            timetableItemStartTime = itemView.findViewById(R.id.timetableItemStartTime);
            timetableItemClassroomName = itemView.findViewById(R.id.timetableItemClassroomName);
            timetableItemTeamName = itemView.findViewById(R.id.timetableItemTeamName);
            timetableItemSubjectChange = itemView.findViewById(R.id.timetableItemSubjectChange);
            timetableItemSubjectName = itemView.findViewById(R.id.timetableItemSubjectName);
            timetableItemTeacherName = itemView.findViewById(R.id.noticesItemTeacherName);
            timetableItemEndTime = itemView.findViewById(R.id.timetableItemEndTime);
            timetableItemEvent1 = itemView.findViewById(R.id.timetableItemEvent1);
            timetableItemEvent2 = itemView.findViewById(R.id.timetableItemEvent2);
            timetableItemEvent3 = itemView.findViewById(R.id.timetableItemEvent3);
        }
    }
}