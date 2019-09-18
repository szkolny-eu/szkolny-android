package pl.szczodrzynski.edziennik.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.adapters.HomeworksAdapter;
import pl.szczodrzynski.edziennik.databinding.FragmentRegisterHomeworksBinding;
import pl.szczodrzynski.edziennik.datamodels.Metadata;
import pl.szczodrzynski.edziennik.dialogs.EventManualDialog;
import pl.szczodrzynski.edziennik.models.Date;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem;

import static pl.szczodrzynski.edziennik.datamodels.Event.TYPE_HOMEWORK;

public class RegisterHomeworksFragment extends Fragment {

    private App app = null;
    private MainActivity activity = null;
    private FragmentRegisterHomeworksBinding b = null;

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_register_homeworks, container, false);
        b.refreshLayout.setParent(activity.getSwipeRefreshLayout());
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        activity.getBottomSheet().prependItems(
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_add_event)
                        .withDescription(R.string.menu_add_event_desc)
                        .withIcon(CommunityMaterial.Icon.cmd_calendar_plus)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            new EventManualDialog(activity).show(app, null, null, null, EventManualDialog.DIALOG_HOMEWORK);
                        }),
                new BottomSheetSeparatorItem(true),
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            AsyncTask.execute(() -> app.db.metadataDao().setAllSeen(App.profileId, Metadata.TYPE_HOMEWORK, true));
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show();
                        })
        );
        activity.gainAttention();

        /*b.refreshLayout.setOnRefreshListener(() -> {
            activity.syncCurrentFeature(MainActivity.DRAWER_ITEM_HOMEWORKS, b.refreshLayout);
        });*/

        LinearLayoutManager linearLayoutCurrentManager = new LinearLayoutManager(getContext());
        linearLayoutCurrentManager.setReverseLayout(false);
        linearLayoutCurrentManager.setStackFromEnd(false);
        LinearLayoutManager linearLayoutPastManager = new LinearLayoutManager(getContext());
        linearLayoutPastManager.setReverseLayout(true);
        linearLayoutPastManager.setStackFromEnd(true);

        b.homeworksCurrentView.setHasFixedSize(true);
        b.homeworksCurrentView.setLayoutManager(linearLayoutCurrentManager);
        b.homeworksPastView.setHasFixedSize(true);
        b.homeworksPastView.setLayoutManager(linearLayoutPastManager);

        app.db.eventDao().getAllByType(App.profileId, TYPE_HOMEWORK, "eventDate >= '"+Date.getToday().getStringY_m_d()+"'").observe(this, currentList -> {
            if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                return;

            if (currentList != null && currentList.size() > 0) {
                HomeworksAdapter adapter = new HomeworksAdapter(getContext(), currentList);
                b.homeworksCurrentView.setAdapter(adapter);
                b.homeworksCurrentView.setVisibility(View.VISIBLE);
                b.homeworksCurrentNoData.setVisibility(View.GONE);
            }
            else {
                b.homeworksCurrentView.setVisibility(View.GONE);
                b.homeworksCurrentNoData.setVisibility(View.VISIBLE);
            }
        });

        app.db.eventDao().getAllByType(App.profileId, TYPE_HOMEWORK, "eventDate < '"+Date.getToday().getStringY_m_d()+"'").observe(this, pastList -> {
            if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                return;

            if (pastList != null && pastList.size() > 0) {
                HomeworksAdapter adapter = new HomeworksAdapter(getContext(), pastList);
                b.homeworksPastView.setAdapter(adapter);
                b.homeworksPastView.setVisibility(View.VISIBLE);
                b.homeworksPastNoData.setVisibility(View.GONE);
            }
            else {
                b.homeworksPastView.setVisibility(View.GONE);
                b.homeworksPastNoData.setVisibility(View.VISIBLE);
            }
        });
    }
}
