package pl.szczodrzynski.edziennik.ui.announcements;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask;
import pl.szczodrzynski.edziennik.data.api.events.AnnouncementGetEvent;
import pl.szczodrzynski.edziennik.data.enums.LoginType;
import pl.szczodrzynski.edziennik.data.enums.MetadataType;
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull;
import pl.szczodrzynski.edziennik.databinding.DialogAnnouncementBinding;
import pl.szczodrzynski.edziennik.databinding.FragmentAnnouncementsBinding;
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem;

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
        // activity, context and profile is valid
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_announcements, container, false);
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
                            if (app.getProfile().getLoginStoreType() == LoginType.LIBRUS) {
                                EdziennikTask.Companion.announcementsRead(App.Companion.getProfileId()).enqueue(requireContext());
                            } else {
                                AsyncTask.execute(() -> App.Companion.getDb().metadataDao().setAllSeen(App.Companion.getProfileId(), MetadataType.ANNOUNCEMENT, true));
                                Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show();
                            }
                        })
        );

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

        //RecyclerViewExpandableItemManager expMgr = new RecyclerViewExpandableItemManager(null);

        recyclerView = b.announcementsView;
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(view.getContext()));

        app.getDb().announcementDao().getAll(App.Companion.getProfileId()).observe(getViewLifecycleOwner(), announcements -> {
            if (app == null || activity == null || b == null || !isAdded())
                return;

            for (AnnouncementFull it : announcements) {
                it.filterNotes();
            }

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
                    if (announcement.getText() == null || (app.getProfile().getLoginStoreType() == LoginType.LIBRUS && !announcement.getSeen())) {
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
        DialogAnnouncementBinding b = DialogAnnouncementBinding.inflate(LayoutInflater.from(activity), null, false);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(announcement.getSubject())
                .setView(b.getRoot())
                .setPositiveButton(R.string.ok, null)
                .show();
        b.text.setText(announcement.getTeacherName() +"\n\n"+ (announcement.getStartDate() != null ? announcement.getStartDate().getFormattedString() : "-") + (announcement.getEndDate() != null ? " do " + announcement.getEndDate().getFormattedString() : "")+"\n\n" +announcement.getText());
        if (!announcement.getSeen() && app.getProfile().getLoginStoreType() != LoginType.LIBRUS) {
            announcement.setSeen(true);
            AsyncTask.execute(() -> App.Companion.getDb().metadataDao().setSeen(App.Companion.getProfileId(), announcement, true));
            if (recyclerView.getAdapter() != null)
                recyclerView.getAdapter().notifyDataSetChanged();
        }
    }
}
