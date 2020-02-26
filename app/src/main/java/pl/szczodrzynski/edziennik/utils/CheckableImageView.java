/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-8.
 */

package pl.szczodrzynski.edziennik.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;

import androidx.appcompat.widget.AppCompatImageView;

public class CheckableImageView extends AppCompatImageView implements Checkable {

    private boolean checked;
    private boolean broadcasting;

    private OnCheckedChangeListener onCheckedChangeListener;

    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };

    public interface OnCheckedChangeListener {
        void onCheckedChanged(CheckableImageView checkableImageView, boolean isChecked);
    }

    public CheckableImageView(final Context context) {
        this(context, null);
    }

    public CheckableImageView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckableImageView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnClickListener(v -> toggle());
    }

    @Override public int[] onCreateDrawableState(final int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }

    @Override public void toggle() {
        setChecked(!checked);
    }

    @Override public boolean isChecked() {
        return checked;
    }

    @Override public void setChecked(final boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            refreshDrawableState();

            // Avoid infinite recursions if setChecked() is called from a listener
            if (broadcasting) {
                return;
            }
            broadcasting = true;
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(this, checked);
            }
            broadcasting = false;
        }
    }

    public void setOnCheckedChangeListener( final OnCheckedChangeListener onCheckedChangeListener) {
        this.onCheckedChangeListener = onCheckedChangeListener;
    }
}
