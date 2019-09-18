package pl.szczodrzynski.edziennik.dialogs;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.ColorInt;
import android.util.TypedValue;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;

public class GenericDialog {
    private App app;
    private Context context;

    public GenericDialog(Context context) {
        this.context = context;
    }

    private MaterialDialog dialog;
    private View dialogView;

    public void positiveButton()
    {

    }

    public void show(App _app)
    {
        this.app = _app;
        dialog = new MaterialDialog.Builder(context)
                .title("Dialog title")
                //.customView(R.layout.dialog_register_exam_add, true)
                .positiveText("Positive")
                .negativeText("Negative")
                .autoDismiss(false)
                .onPositive((dialog, which) -> positiveButton())
                .onNegative((dialog, which) -> dialog.dismiss())
                .show();

        dialogView = dialog.getCustomView();
        assert dialogView != null;

        // USE NavView.TextView.* instead
        /*TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        @ColorInt int primaryTextColor = typedValue.data;*/
    }
}
