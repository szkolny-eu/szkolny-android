package pl.szczodrzynski.edziennik.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;

public class Anim {
    public static void expand(View v, Integer duration, Animation.AnimationListener animationListener) {
        v.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int targetHeight = v.getMeasuredHeight();
        //Log.d("Anim", "targetHeight="+targetHeight);
        v.setVisibility(View.VISIBLE);
        v.getLayoutParams().height = 0;
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1.0f
                        ? LinearLayout.LayoutParams.WRAP_CONTENT//(int)(targetHeight * interpolatedTime)
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        if (duration == null) {
            a.setDuration((long) ((int) (((float) targetHeight) / v.getContext().getResources().getDisplayMetrics().density)));
        } else {
            a.setDuration((long) duration);
        }
        if (animationListener != null ) {
            a.setAnimationListener(animationListener);
        }
        v.startAnimation(a);
    }

    public static void collapse(View v, Integer duration, Animation.AnimationListener animationListener) {
        int initialHeight = v.getMeasuredHeight();
        Animation a = new Animation() {
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1.0f) {
                    v.setVisibility(View.GONE);
                    return;
                }
                v.getLayoutParams().height = initialHeight - ((int) (((float) initialHeight) * interpolatedTime));
                v.requestLayout();
            }

            public boolean willChangeBounds() {
                return true;
            }
        };
        if (duration == null) {
            a.setDuration((long) ((int) (((float) initialHeight) / v.getContext().getResources().getDisplayMetrics().density)));
        } else {
            a.setDuration((long) duration);
        }
        if (animationListener != null ) {
            a.setAnimationListener(animationListener);
        }
        v.startAnimation(a);
    }

    public static void fadeIn(View v, Integer duration, Animation.AnimationListener animationListener) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(duration);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                v.setVisibility(View.VISIBLE);
                if (animationListener != null) {
                    animationListener.onAnimationStart(animation);
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (animationListener != null) {
                    animationListener.onAnimationEnd(animation);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (animationListener != null) {
                    animationListener.onAnimationRepeat(animation);
                }
            }
        });
        v.startAnimation(fadeIn);
    }

    public static void fadeOut(View v, Integer duration, Animation.AnimationListener animationListener) {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setDuration(duration);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (animationListener != null) {
                    animationListener.onAnimationStart(animation);
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.INVISIBLE);
                if (animationListener != null) {
                    animationListener.onAnimationEnd(animation);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (animationListener != null) {
                    animationListener.onAnimationRepeat(animation);
                }
            }
        });
        v.startAnimation(fadeOut);
    }

    public static void scaleView(View v, Integer duration, Animation.AnimationListener animationListener, float startScale, float endScale) {
        Animation anim = new ScaleAnimation(
                1f, 1f, // Start and end values for the X axis scaling
                startScale, endScale, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        anim.setDuration(duration);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (animationListener != null) {
                    animationListener.onAnimationStart(animation);
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (animationListener != null) {
                    animationListener.onAnimationEnd(animation);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (animationListener != null) {
                    animationListener.onAnimationRepeat(animation);
                }
            }
        });
        v.startAnimation(anim);
    }

    public static class ResizeAnimation extends Animation {
        private View mView;
        private float mToHeight;
        private float mFromHeight;

        private float mToWidth;
        private float mFromWidth;

        private float width;
        private float height;

        public ResizeAnimation(View v, float fromWidth, float fromHeight, float toWidth, float toHeight) {
            mToHeight = toHeight;
            mToWidth = toWidth;
            mFromHeight = fromHeight;
            mFromWidth = fromWidth;
            mView = v;
            width = v.getWidth();
            height = v.getHeight();
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height =
                    (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            ViewGroup.LayoutParams p = mView.getLayoutParams();
            p.width = (int) (width * this.width);
            p.height = (int) (height * this.height);
            mView.requestLayout();
        }
    }
}
