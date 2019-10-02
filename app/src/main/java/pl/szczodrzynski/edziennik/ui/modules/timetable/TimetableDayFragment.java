package pl.szczodrzynski.edziennik.ui.modules.timetable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.databinding.FragmentTimetableDayBinding;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

public class TimetableDayFragment extends Fragment {

    private App app = null;
    private MainActivity activity = null;
    private FragmentTimetableDayBinding b = null;

    private RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        if (getActivity() == null || getContext() == null)
            return null;
        app = (App) activity.getApplication();
        getContext().getTheme().applyStyle(Themes.INSTANCE.getAppTheme(), true);
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false);
        // activity, context and profile is valid
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_timetable_day, container, false);
        b.refreshLayout.setParent(activity.getSwipeRefreshLayout());
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        /*b.getRoot().setOnTouchListener((v, event) -> {
            d("TimetableDay", "event "+event);
            event.setSource(0x10000000); // set a unique source
            activity.swipeRefreshLayout.onTouchEvent(event);
            return true;
        });*/
        //b.refreshLayout.setNestedScrollingEnabled(true);
        /*b.refreshLayout.setOnRefreshListener(() -> {
            activity.syncCurrentFeature(MainActivity.DRAWER_ITEM_TIMETABLE, b.refreshLayout);
        });*/

        assert getArguments() != null;
        Date date = new Date().parseFromYmd(Long.toString(getArguments().getLong("date", 20181009)));

        recyclerView = b.timetableView;
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        app.db.lessonDao().getAllByDate(App.profileId, date, Time.getNow()).observe(this, lessons -> {
            if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                return;

            if (lessons != null && lessons.size() > 0) {
                app.db.eventDao().getAllByDate(App.profileId, date).observe(this, events -> {
                    TimetableAdapter adapter = new TimetableAdapter(getContext(), date, lessons, events == null ? new ArrayList<>() : events);
                    recyclerView.setAdapter(adapter);
                    recyclerView.setVisibility(View.VISIBLE);
                    b.timetableNoData.setVisibility(View.GONE);
                });
            }
            else {
                recyclerView.setVisibility(View.GONE);
                b.timetableNoData.setVisibility(View.VISIBLE);
            }
        });
    }
}
