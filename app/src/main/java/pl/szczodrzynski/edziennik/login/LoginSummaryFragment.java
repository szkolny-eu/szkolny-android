package pl.szczodrzynski.edziennik.login;

import androidx.databinding.DataBindingUtil;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginSummaryBinding;
import pl.szczodrzynski.edziennik.databinding.RowLoginProfileListItemBinding;
import pl.szczodrzynski.edziennik.datamodels.Profile;

public class LoginSummaryFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginSummaryBinding b;
    private static final String TAG = "LoginSummary";

    public LoginSummaryFragment() { }

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_summary, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;

        LoginActivity.firstCompleted = true;

        List<ItemProfileModel> profileList = new ArrayList<>();
        int index = 0;
        for (LoginProfileObject profileObject: LoginActivity.profileObjects) {
            int subIndex = 0;
            for (Profile profile: profileObject.profileList) {
                ItemProfileModel profileModel = new ItemProfileModel(
                        index,
                        subIndex,
                        profile.getName(),
                        profileObject.loginStore.type,
                        profileObject.selectedList.get(subIndex)
                );
                profileList.add(profileModel);
                subIndex++;
            }
            index++;
        }

        b.profileListView.setLayoutManager(new LinearLayoutManager(getContext()));
        b.profileListView.setAdapter(new ProfileListAdapter(profileList));

        b.registerMeSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (!isChecked) {
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.login_summary_unregister_title)
                        .content(R.string.login_summary_unregister_text)
                        .positiveText(R.string.yes)
                        .negativeText(R.string.cancel)
                        .onNegative(((dialog, which) -> {
                            b.registerMeSwitch.setChecked(true);
                        }))
                        .show();
            }
        }));

        b.anotherButton.setOnClickListener((v -> nav.navigate(R.id.loginChooserFragment, null, LoginActivity.navOptions)));

        b.finishButton.setOnClickListener(v -> {
            if (LoginActivity.privacyPolicyAccepted) {
                Bundle args = new Bundle();
                args.putBoolean("registrationAllowed", b.registerMeSwitch.isChecked());
                nav.navigate(R.id.loginSyncFragment, args, LoginActivity.navOptions);
                return;
            }
            boolean profileSelected = true;
            for (LoginProfileObject profileObject: LoginActivity.profileObjects) {
                if (profileObject.selectedList.size() == 0 && profileSelected)
                    profileSelected = false;
            }
            if (!profileSelected) {
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.login_summary_no_profiles_title)
                        .content(R.string.login_summary_no_profiles_text)
                        .positiveText(R.string.ok)
                        .show();
                return;
            }
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.privacy_policy)
                    .content(Html.fromHtml("Korzystając z aplikacji potwierdzasz <a href=\"http://szkolny.eu/privacy-policy\">przeczytanie Polityki prywatności</a> i akceptujesz jej postanowienia."))
                    .positiveText(R.string.i_agree)
                    .neutralText(R.string.i_disagree)
                    .onPositive(((dialog, which) -> {
                        Bundle args = new Bundle();
                        args.putBoolean("registrationAllowed", b.registerMeSwitch.isChecked());
                        nav.navigate(R.id.loginSyncFragment, args, LoginActivity.navOptions);
                    }))
                    .show();
        });
    }

    class ItemProfileModel {
        int listIndex;
        int listSubIndex;
        String name;
        int loginType;
        boolean selected;

        public ItemProfileModel(int listIndex, int listSubIndex, String name, int loginType, boolean selected) {
            this.listIndex = listIndex;
            this.listSubIndex = listSubIndex;
            this.name = name;
            this.loginType = loginType;
            this.selected = selected;
        }
    }

    public class ProfileListAdapter extends RecyclerView.Adapter<ProfileListAdapter.ViewHolder> {
        private List<ItemProfileModel> profileList;

        public ProfileListAdapter(List<ItemProfileModel> profileList) {
            this.profileList = profileList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RowLoginProfileListItemBinding b = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.row_login_profile_list_item, parent, false);
            return new ViewHolder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RowLoginProfileListItemBinding b = holder.b;
            ItemProfileModel m = profileList.get(position);

            b.textView.setText(m.name);
            b.checkBox.setChecked(m.selected);
            b.checkBox.jumpDrawablesToCurrentState();
            View.OnClickListener onClickListener = v -> {
                if (v instanceof CheckBox) {
                    m.selected = ((CheckBox) v).isChecked();
                } else {
                    m.selected = !m.selected;
                    b.checkBox.setChecked(m.selected);
                    b.checkBox.jumpDrawablesToCurrentState();
                }
                LoginActivity.profileObjects.get(m.listIndex).selectedList.set(m.listSubIndex, m.selected);
            };
            b.checkBox.setOnClickListener(onClickListener);
            b.getRoot().setOnClickListener(onClickListener);
            //b.root.setOnClickListener(onClickListener);
            //holder.bind(b.textView, onClickListener);
        }


        @Override
        public int getItemCount() {
            return profileList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            RowLoginProfileListItemBinding b;

            public ViewHolder(@NonNull RowLoginProfileListItemBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }
    }
}
