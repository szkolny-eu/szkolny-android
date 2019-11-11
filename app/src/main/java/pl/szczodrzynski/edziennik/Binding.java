/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-11.
 */

package pl.szczodrzynski.edziennik;

import android.graphics.Paint;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;

public class Binding {
    @BindingAdapter("strikeThrough")
    public static void strikeThrough(TextView textView, Boolean strikeThrough) {
        if (strikeThrough) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }
}
