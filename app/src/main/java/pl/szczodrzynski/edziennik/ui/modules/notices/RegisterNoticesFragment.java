package pl.szczodrzynski.edziennik.ui.modules.notices;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.databinding.FragmentRegisterNoticesBinding;
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice;
import pl.szczodrzynski.edziennik.data.db.modules.notices.NoticeFull;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem;

import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_NOTICE;

public class RegisterNoticesFragment extends Fragment {

    private App app = null;
    private MainActivity activity = null;
    private FragmentRegisterNoticesBinding b = null;

    private int displayMode = MODE_YEAR;
    private static final int MODE_YEAR = 0;
    private static final int MODE_SEMESTER_1 = 1;
    private static final int MODE_SEMESTER_2 = 2;

    private List<NoticeFull> noticeList = null;

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_register_notices, container, false);
        b.refreshLayout.setParent(activity.getSwipeRefreshLayout());
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        activity.getBottomSheet().prependItems(
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            AsyncTask.execute(() -> app.db.metadataDao().setAllSeen(App.profileId, TYPE_NOTICE, true));
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show();
                        })
        );

        /*b.refreshLayout.setOnRefreshListener(() -> {
            activity.syncCurrentFeature(MainActivity.DRAWER_ITEM_NOTICES, b.refreshLayout);
        });*/

        b.noticesSummaryTitle.setOnClickListener((v -> {
            PopupMenu popupMenu = new PopupMenu(activity, b.noticesSummaryTitle, Gravity.END);
            popupMenu.getMenu().add(0, 0, 0, R.string.summary_mode_year);
            popupMenu.getMenu().add(0, 1, 1, R.string.summary_mode_semester_1);
            popupMenu.getMenu().add(0, 2, 2, R.string.summary_mode_semester_2);
            popupMenu.setOnMenuItemClickListener((item -> {
                displayMode = item.getItemId();
                updateList();
                return true;
            }));
            popupMenu.show();
        }));

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

        b.noticesView.setHasFixedSize(true);
        b.noticesView.setLayoutManager(linearLayoutManager);

        app.db.noticeDao().getAll(App.profileId).observe(this, notices -> {
            if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                return;

            if (notices == null) {
                b.noticesView.setVisibility(View.GONE);
                b.noticesNoData.setVisibility(View.VISIBLE);
                return;
            }

            noticeList = notices;

            updateList();
        });
    }

    private void updateList() {
        int praisesCount = 0;
        int warningsCount = 0;
        int otherCount = 0;

        List<NoticeFull> filteredList = new ArrayList<>();
        for (NoticeFull notice: noticeList) {
            if (displayMode != MODE_YEAR && notice.semester != displayMode)
                continue;
            filteredList.add(notice);
            switch (notice.type) {
                case Notice.TYPE_POSITIVE:
                    praisesCount++;
                    break;
                case Notice.TYPE_NEGATIVE:
                    warningsCount++;
                    break;
                case Notice.TYPE_NEUTRAL:
                    otherCount++;
                    break;
            }
        }
        if (filteredList.size() > 0) {
            NoticesAdapter adapter;
            b.noticesView.setVisibility(View.VISIBLE);
            b.noticesNoData.setVisibility(View.GONE);
            if ((adapter = (NoticesAdapter) b.noticesView.getAdapter()) != null) {
                adapter.setNoticeList(filteredList);
                adapter.notifyDataSetChanged();
            }
            else {
                adapter = new NoticesAdapter(getContext(), filteredList);
                b.noticesView.setAdapter(adapter);
            }
        }
        else {
            b.noticesView.setVisibility(View.GONE);
            b.noticesNoData.setVisibility(View.VISIBLE);
        }

        if (displayMode == MODE_YEAR) {
            b.noticesSummaryTitle.setText(getString(R.string.notices_summary_title_year));
        }
        else {
            b.noticesSummaryTitle.setText(getString(R.string.notices_summary_title_semester_format, displayMode));
        }
        b.noticesPraisesCount.setText(String.format(Locale.getDefault(), "%d", praisesCount));
        b.noticesWarningsCount.setText(String.format(Locale.getDefault(), "%d", warningsCount));
        b.noticesOtherCount.setText(String.format(Locale.getDefault(), "%d", otherCount));
        if (warningsCount >= 3) {
            b.noticesWarningsCount.setTextColor(Color.RED);
        }
        else {
            b.noticesWarningsCount.setTextColor(Themes.INSTANCE.getPrimaryTextColor(activity));
        }
    }
}
