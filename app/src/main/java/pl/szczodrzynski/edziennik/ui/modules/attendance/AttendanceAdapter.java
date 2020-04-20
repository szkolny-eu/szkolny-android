package pl.szczodrzynski.edziennik.ui.modules.attendance;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull;

import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_ABSENT;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_ABSENT_EXCUSED;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_BELATED;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_BELATED_EXCUSED;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_DAY_FREE;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_PRESENT;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_RELEASED;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {
    private Context context;
    public List<AttendanceFull> attendanceList;

    //getting the context and product list with constructor
    public AttendanceAdapter(Context mCtx, List<AttendanceFull> noticeList) {
        this.context = mCtx;
        this.attendanceList = noticeList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflating and returning our view holder
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_attendance_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        App app = (App) context.getApplicationContext();

        AttendanceFull attendance = attendanceList.get(position);

        holder.attendanceLessonTopic.setText(attendance.lessonTopic);
        holder.attendanceTeacher.setText(attendance.teacherFullName);
        holder.attendanceSubject.setText(attendance.subjectLongName);
        holder.attendanceDate.setText(attendance.lessonDate.getStringDmy());
        holder.attendanceTime.setText(attendance.startTime.getStringHM());

        switch (attendance.type) {
            case TYPE_DAY_FREE:
                holder.attendanceType.getBackground().setColorFilter(new PorterDuffColorFilter(0xff166ee0, PorterDuff.Mode.MULTIPLY));
                holder.attendanceType.setText(R.string.attendance_free_day);
                break;
            case TYPE_ABSENT:
                holder.attendanceType.getBackground().setColorFilter(new PorterDuffColorFilter(0xfff44336, PorterDuff.Mode.MULTIPLY));
                holder.attendanceType.setText(R.string.attendance_absent);
                break;
            case TYPE_ABSENT_EXCUSED:
                holder.attendanceType.getBackground().setColorFilter(new PorterDuffColorFilter(0xffaeea00, PorterDuff.Mode.MULTIPLY));
                holder.attendanceType.setText(R.string.attendance_absent_excused);
                break;
            case TYPE_BELATED:
                holder.attendanceType.getBackground().setColorFilter(new PorterDuffColorFilter(0xffffca28, PorterDuff.Mode.MULTIPLY));
                holder.attendanceType.setText(R.string.attendance_belated);
                break;
            case TYPE_BELATED_EXCUSED:
                holder.attendanceType.getBackground().setColorFilter(new PorterDuffColorFilter(0xff4bb733, PorterDuff.Mode.MULTIPLY));
                holder.attendanceType.setText(R.string.attendance_belated_excused);
                break;
            case TYPE_RELEASED:
                holder.attendanceType.getBackground().setColorFilter(new PorterDuffColorFilter(0xff9e9e9e, PorterDuff.Mode.MULTIPLY));
                holder.attendanceType.setText(R.string.attendance_released);
                break;
            case TYPE_PRESENT:
                holder.attendanceType.getBackground().setColorFilter(new PorterDuffColorFilter(0xffffae00, PorterDuff.Mode.MULTIPLY));
                holder.attendanceType.setText(R.string.attendance_present);
                break;
            default:
                holder.attendanceType.getBackground().setColorFilter(new PorterDuffColorFilter(0xff03a9f4, PorterDuff.Mode.MULTIPLY));
                holder.attendanceType.setText("?");
                break;
        }

        if (!attendance.seen) {
            holder.attendanceLessonTopic.setBackground(context.getResources().getDrawable(R.drawable.bg_rounded_8dp));
            holder.attendanceLessonTopic.getBackground().setColorFilter(new PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY));
            attendance.seen = true;
            AsyncTask.execute(() -> {
                App.db.metadataDao().setSeen(App.Companion.getProfileId(), attendance, true);
                //Intent i = new Intent("android.intent.action.MAIN").putExtra(MainActivity.ACTION_UPDATE_BADGES, "yes, sure");
                //context.sendBroadcast(i);
            });
        }
        else {
            holder.attendanceLessonTopic.setBackground(null);
        }
    }

    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView attendanceType;
        TextView attendanceLessonTopic;
        TextView attendanceSubject;
        TextView attendanceTeacher;
        TextView attendanceDate;
        TextView attendanceTime;

        ViewHolder(View itemView) {
            super(itemView);
            attendanceType = itemView.findViewById(R.id.attendanceType);
            attendanceLessonTopic = itemView.findViewById(R.id.attendanceLessonTopic);
            attendanceSubject = itemView.findViewById(R.id.attendanceSubject);
            attendanceTeacher = itemView.findViewById(R.id.attendanceTeacher);
            attendanceDate = itemView.findViewById(R.id.attendanceDate);
            attendanceTime = itemView.findViewById(R.id.attendanceTime);
        }
    }
}
