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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask;
import pl.szczodrzynski.edziennik.data.api.events.AnnouncementGetEvent;
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull;
import pl.szczodrzynski.edziennik.databinding.DialogAnnouncementBinding;
import pl.szczodrzynski.edziennik.databinding.FragmentAnnouncementsBinding;
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static pl.szczodrzynski.edziennik.data.db.entity.LoginStore.LOGIN_TYPE_LIBRUS;
import static pl.szczodrzynski.edziennik.data.db.entity.Metadata.TYPE_ANNOUNCEMENT;

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
        // activity, context and profile is valid
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_announcements, container, false);
        b.refreshLayout.setParent(activity.getSwipeRefreshLayout());
        return b.getRoot();
    }

    private RecyclerView recyclerView;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || activity == null || b == null || !isAdded())
            return;

        activity.getBottomSheet().prependItems(
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            if (app.getProfile().getLoginStoreType() == LOGIN_TYPE_LIBRUS) {
                                EdziennikTask.Companion.announcementsRead(App.Companion.getProfileId()).enqueue(requireContext());
                            } else {
                                AsyncTask.execute(() -> App.db.metadataDao().setAllSeen(App.Companion.getProfileId(), TYPE_ANNOUNCEMENT, true));
                                Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show();
                            }
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

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (recyclerView.canScrollVertically(-1)) {
                    b.refreshLayout.setEnabled(false);
                }
                if (!recyclerView.canScrollVertically(-1) && newState == SCROLL_STATE_IDLE) {
                    b.refreshLayout.setEnabled(true);
                }
            }
        });

        app.db.announcementDao().getAll(App.Companion.getProfileId()).observe(this, announcements -> {
            if (app == null || activity == null || b == null || !isAdded())
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
                    if (announcement.getText() == null || (app.getProfile().getLoginStoreType() == LOGIN_TYPE_LIBRUS && !announcement.getSeen())) {
                        EdziennikTask.Companion.announcementGet(App.Companion.getProfileId(), announcement).enqueue(requireContext());
                    } else {
                        showAnnouncementDetailsDialog(announcement);
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

    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onAnnouncementGetEvent(AnnouncementGetEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        showAnnouncementDetailsDialog(event.getAnnouncement());
    }

    private void showAnnouncementDetailsDialog(AnnouncementFull announcement) {
        MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .title(announcement.getSubject())
                .customView(R.layout.dialog_announcement, true)
                .positiveText(R.string.ok)
                .show();
        DialogAnnouncementBinding b = DialogAnnouncementBinding.bind(dialog.getCustomView());
        b.text.setText(announcement.getTeacherName() +"\n\n"+ (announcement.getStartDate() != null ? announcement.getStartDate().getFormattedString() : "-") + (announcement.getEndDate() != null ? " do " + announcement.getEndDate().getFormattedString() : "")+"\n\n" +announcement.getText());
        if (!announcement.getSeen() && app.getProfile().getLoginStoreType() != LOGIN_TYPE_LIBRUS) {
            announcement.setSeen(true);
            AsyncTask.execute(() -> App.db.metadataDao().setSeen(App.Companion.getProfileId(), announcement, true));
            if (recyclerView.getAdapter() != null)
                recyclerView.getAdapter().notifyDataSetChanged();
        }
    }
}
