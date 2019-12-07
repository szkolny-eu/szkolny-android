package pl.szczodrzynski.edziennik.ui.modules.announcements;

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
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.databinding.DialogAnnouncementBinding;
import pl.szczodrzynski.edziennik.databinding.FragmentAnnouncementsBinding;
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem;

import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_ANNOUNCEMENT;

public class AnnouncementsFragment extends Fragment {

    private App app = null;
    private MainActivity activity = null;
    private FragmentAnnouncementsBinding b = null;

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_announcements, container, false);
        b.refreshLayout.setParent(activity.getSwipeRefreshLayout());
        return b.getRoot();
    }

    private RecyclerView recyclerView;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        activity.getBottomSheet().prependItems(
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            AsyncTask.execute(() -> app.db.metadataDao().setAllSeen(App.profileId, TYPE_ANNOUNCEMENT, true));
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show();
                        })
        );

        /*b.refreshLayout.setOnRefreshListener(() -> {
            activity.syncCurrentFeature(MainActivity.DRAWER_ITEM_ANNOUNCEMENTS, b.refreshLayout);
        });*/

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

        //RecyclerViewExpandableItemManager expMgr = new RecyclerViewExpandableItemManager(null);

        recyclerView = b.announcementsView;
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(view.getContext()));

        app.db.announcementDao().getAll(App.profileId).observe(this, announcements -> {
            if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                return;

            if (announcements == null) {
                recyclerView.setVisibility(View.GONE);
                b.announcementsNoData.setVisibility(View.VISIBLE);
                return;
            }

            if (announcements.size() > 0) {
                /*if ((adapter = (AnnouncementsAdapter) recyclerView.getAdapter()) != null) {
                    adapter.announcementList = announcements;
                    adapter.notifyDataSetChanged();
                    return;
                }*/
                AnnouncementsAdapter announcementsAdapter = new AnnouncementsAdapter(activity, announcements, (v, announcement) -> {
                    MaterialDialog dialog = new MaterialDialog.Builder(activity)
                            .title(announcement.subject)
                            .customView(R.layout.dialog_announcement, true)
                            .positiveText(R.string.ok)
                            .show();
                    DialogAnnouncementBinding b = DialogAnnouncementBinding.bind(dialog.getCustomView());
                    b.text.setText(announcement.teacherFullName+"\n\n"+ (announcement.startDate != null ? announcement.startDate.getFormattedString() : "-")+" do "+ (announcement.endDate != null ? announcement.endDate.getFormattedString() : "-")+"\n\n" +announcement.text);
                    if (!announcement.seen) {
                        announcement.seen = true;
                        AsyncTask.execute(() -> {
                            app.db.metadataDao().setSeen(App.profileId, announcement, true);
                        });
                        if (recyclerView.getAdapter() != null)
                            recyclerView.getAdapter().notifyDataSetChanged();
                    }
                });

                recyclerView.setAdapter(announcementsAdapter);
                // NOTE: need to disable change animations to ripple effect work properly
                //((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
                //expMgr.attachRecyclerView(recyclerView);

                recyclerView.setVisibility(View.VISIBLE);
                b.announcementsNoData.setVisibility(View.GONE);
            }
            else {
                recyclerView.setVisibility(View.GONE);
                b.announcementsNoData.setVisibility(View.VISIBLE);
            }
        });
    }
}
