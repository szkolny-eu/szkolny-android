package pl.szczodrzynski.edziennik.utils;

import android.content.Context;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;

import androidx.core.view.ViewCompat;
import pl.szczodrzynski.edziennik.R;

public class SnackbarHelper {

    public static void configSnackbar(Context context, Snackbar snack) {
        addMargins(snack);
        setRoundBordersBg(context, snack);
        ViewCompat.setElevation(snack.getView(), 6f);
    }

    private static void addMargins(Snackbar snack) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snack.getView().getLayoutParams();
        params.setMargins(12, 12, 12, 12);
        snack.getView().setLayoutParams(params);
    }

    private static void setRoundBordersBg(Context context, Snackbar snackbar) {
        snackbar.getView().setBackground(context.getResources().getDrawable(R.drawable.bg_snackbar));
    }
}
