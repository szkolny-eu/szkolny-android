package pl.szczodrzynski.edziennik.ui.modules.login;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.ExtensionsKt;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginSummaryBinding;
import pl.szczodrzynski.edziennik.databinding.RowLoginProfileListItemBinding;

import static pl.szczodrzynski.edziennik.data.api.LoginMethodsKt.LOGIN_MODE_LIBRUS_EMAIL;
import static pl.szczodrzynski.edziennik.data.api.LoginMethodsKt.LOGIN_MODE_VULCAN_API;
import static pl.szczodrzynski.edziennik.data.api.LoginMethodsKt.LOGIN_MODE_VULCAN_WEB;
import static pl.szczodrzynski.edziennik.data.api.LoginMethodsKt.LOGIN_TYPE_EDUDZIENNIK;
import static pl.szczodrzynski.edziennik.data.api.LoginMethodsKt.LOGIN_TYPE_IDZIENNIK;
import static pl.szczodrzynski.edziennik.data.api.LoginMethodsKt.LOGIN_TYPE_LIBRUS;
import static pl.szczodrzynski.edziennik.data.api.LoginMethodsKt.LOGIN_TYPE_MOBIDZIENNIK;
import static pl.szczodrzynski.edziennik.data.api.LoginMethodsKt.LOGIN_TYPE_VULCAN;

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
                List<String> subnameList = new ArrayList<>();
                if (profile.getStudentClassName() != null)
                    subnameList.add(profile.getStudentClassName());
                if (profile.getStudentSchoolYear() != null)
                    subnameList.add(profile.getStudentSchoolYear());
                ItemProfileModel profileModel = new ItemProfileModel(
                        index,
                        subIndex,
                        profile.getName(),
                        ExtensionsKt.join(subnameList, " - "),
                        profileObject.loginStore.type,
                        profileObject.loginStore.mode,
                        profile.getAccountNameLong() != null,
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
        String subname;
        int loginType;
        int loginMode;
        boolean isParent;
        boolean selected;

        public ItemProfileModel(int listIndex, int listSubIndex, String name, String subname, int loginType, int loginMode, boolean isParent, boolean selected) {
            this.listIndex = listIndex;
            this.listSubIndex = listSubIndex;
            this.name = name;
            this.subname = subname;
            this.loginType = loginType;
            this.loginMode = loginMode;
            this.isParent = isParent;
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
            int imageRes = 0;
            if (m.loginType == LOGIN_TYPE_MOBIDZIENNIK) {
                imageRes = R.drawable.logo_mobidziennik;
            }
            else if (m.loginType == LOGIN_TYPE_IDZIENNIK) {
                imageRes = R.drawable.logo_idziennik;
            }
            else if (m.loginType == LOGIN_TYPE_LIBRUS) {
                if (m.loginMode == LOGIN_MODE_LIBRUS_EMAIL) {
                    imageRes = R.drawable.logo_librus;
                }
                else {
                    imageRes = R.drawable.logo_synergia;
                }
            }
            else if (m.loginType == LOGIN_TYPE_VULCAN) {
                if (m.loginMode == LOGIN_MODE_VULCAN_WEB) {
                    imageRes = R.drawable.logo_vulcan;
                }
                else if (m.loginMode == LOGIN_MODE_VULCAN_API) {
                    imageRes = R.drawable.logo_dzienniczek;
                }
            }
            else if (m.loginType == LOGIN_TYPE_EDUDZIENNIK) {
                imageRes = R.drawable.logo_edudziennik;
            }
            if (imageRes != 0) {
                b.registerIcon.setImageResource(imageRes);
            }
            if (m.isParent) {
                b.accountType.setText(R.string.login_summary_account_parent);
            }
            else {
                b.accountType.setText(R.string.login_summary_account_child);
            }
            if (m.subname.trim().isEmpty()) {
                b.textDetails.setText(null);
                b.textDetails.setVisibility(View.GONE);
            }
            else {
                b.textDetails.setText(m.subname);
                b.textDetails.setVisibility(View.VISIBLE);
            }
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
