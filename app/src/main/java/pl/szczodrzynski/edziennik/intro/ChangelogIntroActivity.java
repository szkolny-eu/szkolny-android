package pl.szczodrzynski.edziennik.intro;

import android.os.Build;
import android.os.Bundle;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.R;

public class ChangelogIntroActivity extends IntroActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        App app = (App)getApplication();
        overridePendingTransition(R.anim.fade_in, 0);
        super.onCreate(savedInstanceState);
        setButtonBackVisible(true);
        setButtonBackFunction(BUTTON_BACK_FUNCTION_BACK);
        setButtonNextVisible(true);
        setButtonNextFunction(BUTTON_NEXT_FUNCTION_NEXT_FINISH);
        setButtonCtaVisible(false);
        setPageScrollDuration(500);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setPageScrollInterpolator(android.R.interpolator.fast_out_slow_in);
        }

        /*if (app.appConfig.lastAppVersion < 120) {
            addSlide(new SimpleSlide.Builder()
                    .title(R.string.intro_web_push_title)
                    .description(R.string.intro_web_push_text)
                    .image(R.drawable.intro_web_push)
                    .background(R.color.introPage5Color)
                    .backgroundDark(R.color.introPage5ColorDark)
                    .buttonCtaLabel(R.string.ok)
                    .buttonCtaClickListener(v -> nextSlide())
                    .scrollable(true)
                    .build());
        }

        if (app.appConfig.lastAppVersion < 141) {
            addSlide(new SimpleSlide.Builder()
                    .title(R.string.intro_grades_editor_title)
                    .description(R.string.intro_grades_editor_text)
                    .image(R.drawable.intro_grades_editor)
                    .background(R.color.introPage5Color)
                    .backgroundDark(R.color.introPage5ColorDark)
                    .buttonCtaLabel(R.string.ok)
                    .buttonCtaClickListener(v -> nextSlide())
                    .scrollable(true)
                    .build());
        }

        if (app.appConfig.lastAppVersion < 150) {
            addSlide(new SimpleSlide.Builder()
                    .title(R.string.intro_manual_events_title)
                    .description(R.string.intro_manual_events_text)
                    .image(R.drawable.intro_manual_events)
                    .background(R.color.introPage5Color)
                    .backgroundDark(R.color.introPage5ColorDark)
                    .buttonCtaLabel(R.string.ok)
                    .buttonCtaClickListener(v -> nextSlide())
                    .scrollable(true)
                    .build());
        }

        if (app.appConfig.lastAppVersion < 170) {
            addSlide(new SimpleSlide.Builder()
                    .title(R.string.intro_synergia_title)
                    .description(R.string.intro_synergia_text)
                    .image(R.drawable.intro_synergia)
                    .background(R.color.introPage5Color)
                    .backgroundDark(R.color.introPage5ColorDark)
                    .buttonCtaLabel(R.string.ok)
                    .buttonCtaClickListener(v -> nextSlide())
                    .scrollable(true)
                    .build());
        }*/

        app.appConfig.lastAppVersion = BuildConfig.VERSION_CODE;
        app.appConfig.savePending = true;
    }
}
