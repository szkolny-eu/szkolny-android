package pl.szczodrzynski.edziennik.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


public class SwipeRefreshLayoutNoTouch extends SwipeRefreshLayout {
    public SwipeRefreshLayoutNoTouch(@NonNull Context context) {
        super(context);
    }

    public SwipeRefreshLayoutNoTouch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getSource() == 0x10000000) {
            // forward the event to super
            return super.onInterceptTouchEvent(ev);
        }
        // discard all the other events
        return false;

        /*if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL)
            return false;
        super.onInterceptTouchEvent(ev);
        return false;*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getSource() == 0x10000000) {
            // forward the event to super
            return super.onTouchEvent(ev);
        }
        // discard all the other events
        return false;
    }
}
