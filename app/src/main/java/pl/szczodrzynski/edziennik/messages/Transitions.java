package pl.szczodrzynski.edziennik.messages;

import android.animation.TimeInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.TransitionSet;

public class Transitions extends TransitionSet {
    @NonNull
    @Override
    public TransitionSet setInterpolator(@Nullable TimeInterpolator interpolator) {
        for (int i = 0; i < getTransitionCount(); i++) {
            getTransitionAt(i).setInterpolator(interpolator);
        }
        return this;
    }
}
