package pl.szczodrzynski.edziennik.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginFinishBinding;

public class LoginFinishFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginFinishBinding b;
    private static final String TAG = "LoginFinishFragment";
    static boolean firstRun = true;
    static int firstProfileId = -1;

    public LoginFinishFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (getActivity() != null) {
            app = (App) getActivity().getApplicationContext();
            nav = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        }
        else {
            return null;
        }
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_finish, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;

        if (!firstRun) {
            b.loginFinishSubtitle.setText(R.string.login_finish_subtitle_not_first_run);
        }

        b.finishButton.setOnClickListener((v -> {
            Intent intent = null;
            if (firstProfileId != -1) {
                intent = new Intent();
                intent.putExtra("profileId", firstProfileId);
                intent.putExtra("fragmentId", MainActivity.DRAWER_ITEM_HOME);
            }
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }));
    }
}
