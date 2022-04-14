/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.szczodrzynski.edziennik.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * The SwipeRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The SwipeRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and
 * progress animation, call setEnabled(false) on the view.
 * <p>
 * This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This view will
 * also be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The SwipeRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.
 * </p>
 */
public class SwipeRefreshLayoutNoIndicator extends SwipeRefreshLayout {

    private SwipeRefreshLayoutNoTouch parent;

    @Override
    public void setEnabled(boolean enabled) {
        if (parent == null)
            return;
        parent.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    public void setParent(SwipeRefreshLayoutNoTouch parent) {
        this.parent = parent;
    }

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public SwipeRefreshLayoutNoIndicator(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public SwipeRefreshLayoutNoIndicator(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean parentConsumed = parent.onInterceptTouchEvent(ev);
        boolean superConsumed = super.onInterceptTouchEvent(ev);
        return parentConsumed && superConsumed;
        /*if (super.onInterceptTouchEvent(ev))
            return parent.onInterceptTouchEvent(ev);
        return false;*/
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        parent.requestDisallowInterceptTouchEvent(b);
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        parent.onStartNestedScroll(child, target, nestedScrollAxes);
        return !parent.isRefreshing() && super.onStartNestedScroll(child, target, nestedScrollAxes);
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        parent.onNestedScrollAccepted(child, target, axes);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        parent.onNestedPreScroll(target, dx, dy, consumed);
    }

    @Override
    public int getNestedScrollAxes() {
        return parent.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        parent.onStopNestedScroll(target);
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        parent.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        if (parent == null)
            return;
        //parent.setNestedScrollingEnabled(enabled);
        super.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return parent.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return parent.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        parent.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return parent.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return super.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return parent.onNestedPreFling(target, velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return parent.onNestedFling(target, velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return parent.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return parent.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        ev.setSource(0x10000000);
        /*boolean consumed = super.onTouchEvent(ev);
        if (consumed) {
            return false;
        }*/
        return parent.onTouchEvent(ev);
    }


}
