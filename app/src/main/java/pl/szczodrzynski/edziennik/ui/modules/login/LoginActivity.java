package pl.szczodrzynski.edziennik.ui.modules.login;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.api.v2.models.ApiError;
import pl.szczodrzynski.edziennik.databinding.ActivityLoginBinding;
import pl.szczodrzynski.edziennik.ui.dialogs.error.ErrorSnackbar;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;
    private App app;
    private static final String TAG = "LoginActivity";
    public static final int RESULT_OK = 1;
    public static NavOptions navOptions;

    static ApiError error = null;
    ErrorSnackbar errorSnackbar = new ErrorSnackbar(this);

    static List<LoginProfileObject> profileObjects;
    public static boolean firstCompleted = false; // if a profile is already added during *this* login. This means that LoginChooser has to navigateUp onBackPressed. Else, finish the activity.
    public static boolean privacyPolicyAccepted = false;

    @Override
    public void onBackPressed() {
        NavDestination destination = Navigation.findNavController(this, R.id.nav_host_fragment).getCurrentDestination();
        if (destination != null && destination.getId() == R.id.loginSyncErrorFragment) {
            return;
        }
        if (destination != null && destination.getId() == R.id.loginProgressFragment) {
            return;
        }
        if (destination != null && destination.getId() == R.id.loginSyncFragment) {
            return;
        }
        if (destination != null && destination.getId() == R.id.loginChooserFragment && !firstCompleted) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        if (destination != null && destination.getId() == R.id.loginSummaryFragment) {
            new MaterialDialog.Builder(this)
                    .title(R.string.are_you_sure)
                    .content(R.string.login_cancel_confirmation)
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .onPositive((dialog, which) -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    })
                    .show();
            return;
        }
        Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.AppTheme_Light);

        firstCompleted = false;
        profileObjects = new ArrayList<>();
        error = null;

        navOptions = new NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build();

        b = DataBindingUtil.inflate(getLayoutInflater(), R.layout.activity_login, null, false);
        setContentView(b.getRoot());

        errorSnackbar.setCoordinator(b.coordinator, null);

        app = (App) getApplication();

        if (!app.appConfig.loginFinished) {
            app.appConfig.miniDrawerVisible = getResources().getConfiguration().smallestScreenWidthDp > 480;
            app.saveConfig("miniDrawerVisible");
        }

        /*b.getRoot().addOnLayoutChangeListener(((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            Animator circularReveal = null;
            float finalRadius = (float) (Math.max(b.revealView.getWidth(), b.revealView.getHeight()) * 1.1);
            circularReveal = ViewAnimationUtils.createCircularReveal(b.revealView, b.revealView.getWidth()/2, b.revealView.getHeight()/2, 0, finalRadius);
            circularReveal.setDuration(400);
            circularReveal.setInterpolator(new AccelerateInterpolator());
            // make the view visible and start the animation
            b.revealView.setVisibility(View.VISIBLE);
            circularReveal.addListener(new Animator.AnimatorListener() {
                @Override public void onAnimationEnd(Animator animation) {
                    Anim.fadeIn(b.title1, 500, new Animation.AnimationListener() {
                        @Override public void onAnimationEnd(Animation animation) {
                            b.title2.postDelayed(() -> {
                                Anim.fadeIn(b.title2, 500, new Animation.AnimationListener() {
                                    @Override public void onAnimationEnd(Animation animation) {
                                        b.revealView.postDelayed(() -> {
                                            Anim.fadeOut(b.title1, 500, null);
                                            Animation anim2 = null;
                                            anim2 = new Anim.ResizeAnimation(b.revealView, 1.0f, 1.0f, 1.0f, 0.15f);
                                            anim2.setDuration(800);
                                            anim2.setInterpolator(new AccelerateDecelerateInterpolator());
                                            b.revealView.startAnimation(anim2);
                                        }, 700);
                                    }
                                    @Override public void onAnimationStart(Animation animation) { }
                                    @Override public void onAnimationRepeat(Animation animation) { }
                                });
                            }, 1500);
                        }
                        @Override public void onAnimationStart(Animation animation) { }
                        @Override public void onAnimationRepeat(Animation animation) { }
                    });
                }
                @Override public void onAnimationCancel(Animator animation) { }
                @Override public void onAnimationStart(Animator animation) { }
                @Override public void onAnimationRepeat(Animator animation) { }
            });
            circularReveal.start();
        }));*/

        /**/

        /*TextInputEditText e = findViewById(R.id.buttontest);

        e.setOnClickListener((v -> {
            Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show();
            PopupMenu popup = new PopupMenu(this, e);
            //popup.getMenu().add(0, 15, 0, HomeFragment.plural(c, R.plurals.time_till_seconds, 15));
            //popup.getMenu().add(0, 15 * 60, 0, HomeFragment.plural(c, R.plurals.time_till_minutes, 15));
            popup.getMenu().add(0, 30 * 60, 0, HomeFragment.plural(this, R.plurals.time_till_minutes, 30));
            popup.getMenu().add(0, 60 * 60, 0, HomeFragment.plural(this, R.plurals.time_till_hours, 1));
            popup.getMenu().add(0, 120 * 60, 0, HomeFragment.plural(this, R.plurals.time_till_hours, 2));
            popup.getMenu().add(0, 180 * 60, 0, HomeFragment.plural(this, R.plurals.time_till_hours, 3));
            popup.getMenu().add(0, 240 * 60, 0, HomeFragment.plural(this, R.plurals.time_till_hours, 4));
            popup.setOnMenuItemClickListener(item -> {
                e.setText(item.getTitle());
                return false;
            });
            popup.show();
        }));*/


    }
}
