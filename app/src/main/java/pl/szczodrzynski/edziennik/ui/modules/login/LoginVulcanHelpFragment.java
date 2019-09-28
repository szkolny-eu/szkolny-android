package pl.szczodrzynski.edziennik.ui.modules.login;

import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginVulcanHelpBinding;

public class LoginVulcanHelpFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginVulcanHelpBinding b;
    private static final String TAG = "LoginVulcanHelp";

    public LoginVulcanHelpFragment() { }

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_vulcan_help, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;

        b.backButton.setOnClickListener((v) -> nav.navigateUp());
    }
}
