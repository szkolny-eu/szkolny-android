package pl.szczodrzynski.edziennik.messages;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.TransitionValues;
import androidx.transition.Visibility;

public class SlideExplode extends Visibility {
    private static final String KEY_SCREEN_BOUNDS = "screenBounds";

    private int[] mTempLoc = new int[2];

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        view.getLocationOnScreen(mTempLoc);
        int left = mTempLoc[0];
        int top = mTempLoc[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        transitionValues.values.put(KEY_SCREEN_BOUNDS, new Rect(left, top, right, bottom));
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        if (endValues == null)
            return null;

        Rect bounds = (Rect) endValues.values.get(KEY_SCREEN_BOUNDS);
        float endY = view.getTranslationY();
        float startY = endY + calculateDistance(sceneRoot, bounds);
        return ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null)
            return null;

        Rect bounds = (Rect) startValues.values.get(KEY_SCREEN_BOUNDS);
        float startY = view.getTranslationY();
        float endY = startY + calculateDistance(sceneRoot, bounds);
        return ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY);
    }

    private int calculateDistance(View sceneRoot, Rect viewBounds) {
        sceneRoot.getLocationOnScreen(mTempLoc);
        int sceneRootY = mTempLoc[1];
        if (getEpicenter() == null) {
            return -sceneRoot.getHeight();
        }
        else if (viewBounds.top <= getEpicenter().top) {
            return sceneRootY - getEpicenter().top;
        }
        else {
            return sceneRootY + sceneRoot.getHeight() - getEpicenter().bottom;
        }
    }
}
