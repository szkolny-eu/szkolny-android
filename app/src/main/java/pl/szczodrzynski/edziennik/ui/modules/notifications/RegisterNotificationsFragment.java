package pl.szczodrzynski.edziennik.ui.modules.notifications;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.databinding.FragmentRegisterNotificationsBinding;
import pl.szczodrzynski.edziennik.utils.Themes;

public class RegisterNotificationsFragment extends Fragment {

    private App app = null;
    private Activity activity = null;
    private FragmentRegisterNotificationsBinding b = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        if (getActivity() == null || getContext() == null)
            return null;
        app = (App) activity.getApplication();
        getContext().getTheme().applyStyle(Themes.INSTANCE.getAppTheme(), true);
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false);
        // activity, context and profile is valid
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_register_notifications, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        RecyclerView recyclerView = b.notificationsView;
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (app.appConfig.notifications.size() > 0) {
            NotificationsAdapter adapter = new NotificationsAdapter(getContext(), app.appConfig.notifications);
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(View.VISIBLE);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
            //linearLayoutManager.setReverseLayout(true);
            //linearLayoutManager.setStackFromEnd(true);
            recyclerView.setLayoutManager(linearLayoutManager);
            b.notificationsNoData.setVisibility(View.GONE);
        }
        else {
            recyclerView.setVisibility(View.GONE);
            b.notificationsNoData.setVisibility(View.VISIBLE);
        }
    }
}
