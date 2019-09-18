package pl.szczodrzynski.edziennik.utils;

import android.content.Context;
import com.google.android.material.textfield.TextInputEditText;

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.core.graphics.drawable.DrawableCompat;

import pl.szczodrzynski.edziennik.R;

public class TextInputDropDown extends TextInputEditText {
    public TextInputDropDown(Context context) {
        super(context);
        create(context);
    }

    public TextInputDropDown(Context context, AttributeSet attrs) {
        super(context, attrs);
        create(context);
    }

    public TextInputDropDown(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create(context);
    }

    public void create(Context context) {
        Drawable drawable = context.getResources().getDrawable(R.drawable.dropdown_arrow);
        Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrappedDrawable, Themes.INSTANCE.getPrimaryTextColor(context));

        setCompoundDrawablesWithIntrinsicBounds(null, null, wrappedDrawable, null);
        setFocusableInTouchMode(false);
        setCursorVisible(false);
        setLongClickable(false);
        setMaxLines(1);
        setInputType(0);
        setKeyListener(null);
        setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                v.setFocusableInTouchMode(false);
            }
        });
    }

    public final void setOnClickListener(OnClickListener onClickListener) {
        super.setOnClickListener(v -> {
            setFocusableInTouchMode(true);
            requestFocus();
            onClickListener.onClick(v);
        });
    }
}
