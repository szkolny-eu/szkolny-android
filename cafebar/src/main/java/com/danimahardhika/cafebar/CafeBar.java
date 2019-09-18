package com.danimahardhika.cafebar;

/*
 * CafeBar
 *
 * Copyright (c) 2017 Dani Mahardhika
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

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import androidx.annotation.BoolRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

@SuppressWarnings("unused")
public class CafeBar {

    static final String FONT_CONTENT = "content";
    static final String FONT_POSITIVE = "positive";
    static final String FONT_NEGATIVE = "negative";
    static final String FONT_NEUTRAL = "neutral";

    private Builder mBuilder;
    private Snackbar mSnackBar;

    private CafeBar(@NonNull Builder builder) {
        mBuilder = builder;

        View baseLayout = mBuilder.mCustomView;
        if (baseLayout == null) {
            LogUtil.d("CafeBar doesn't have customView, preparing it ...");
            baseLayout = CafeBarUtil.getBaseCafeBarView(mBuilder);
        }

        mSnackBar = CafeBarUtil.getBaseSnackBar(baseLayout, mBuilder);
        if (mSnackBar == null) {
            mBuilder = null;
            throw new IllegalStateException("CafeBar base is null");
        }

        if (mBuilder.mCustomView != null) {
            LogUtil.d("CafeBar has custom view, set buttons ignored");
            return;
        }

        if (mBuilder.mPositiveText == null && mBuilder.mNegativeText == null) {
            //Only contains neutral button
            if (mBuilder.mNeutralText != null) {
                int neutralColor = CafeBarUtil.getAccentColor(mBuilder.mContext, mBuilder.mNegativeColor);
                setAction(mBuilder.mNeutralText, neutralColor, mBuilder.mNeutralCallback);
            }
        } else {
            //Contains positive or negative button
            LinearLayout root = (LinearLayout) getView();
            LinearLayout buttonBase = root.findViewById(R.id.cafebar_button_base);

            if (mBuilder.mNeutralText != null) {
                TextView neutral = buttonBase.findViewById(R.id.cafebar_button_neutral);
                neutral.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mBuilder.mNeutralCallback != null) {
                            mBuilder.mNeutralCallback.OnClick(getCafeBar());
                            return;
                        }

                        dismiss();
                    }
                });
            }

            if (mBuilder.mNegativeText != null) {
                TextView negative = buttonBase.findViewById(R.id.cafebar_button_negative);
                negative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mBuilder.mNegativeCallback != null) {
                            mBuilder.mNegativeCallback.OnClick(getCafeBar());
                            return;
                        }

                        dismiss();
                    }
                });
            }

            if (mBuilder.mPositiveText != null) {
                TextView positive = buttonBase.findViewById(R.id.cafebar_button_positive);
                positive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mBuilder.mPositiveCallback != null) {
                            mBuilder.mPositiveCallback.OnClick(getCafeBar());
                            return;
                        }

                        dismiss();
                    }
                });
            }
        }
    }

    public static void enableLogging(boolean enableLogging) {
        LogUtil.sEnableLogging = enableLogging;
    }

    @NonNull
    public static CafeBar make(@NonNull Context context, @StringRes int res, @Snackbar.Duration int duration) {
        String string = context.getResources().getString(res);
        return create(context, null, string, duration);
    }

    @NonNull
    public static CafeBar make(@NonNull Context context, @NonNull String content, @Snackbar.Duration int duration) {
        return create(context, null, content, duration);
    }

    @NonNull
    public static CafeBar make(@NonNull View to, @StringRes int res, @Snackbar.Duration int duration) {
        Context context = to.getContext();
        if (context instanceof ContextThemeWrapper) {
            context = ((ContextThemeWrapper) context).getBaseContext();
        }
        String string = context.getResources().getString(res);
        return create(context, to, string, duration);
    }

    @NonNull
    public static CafeBar make(@NonNull View to, @NonNull String content, @Snackbar.Duration int duration) {
        Context context = to.getContext();
        if (context instanceof ContextThemeWrapper) {
            context = ((ContextThemeWrapper) context).getBaseContext();
        }
        return create(context, to, content, duration);
    }

    @NonNull
    private static CafeBar create(@NonNull Context context, @Nullable View to, @NonNull String content, @Snackbar.Duration int duration) {
        CafeBar.Builder builder = new Builder(context);
        builder.to(to);
        builder.content(content);
        builder.duration(duration);
        if (duration == Snackbar.LENGTH_INDEFINITE) {
            builder.autoDismiss(false);
        }
        return new CafeBar(builder);
    }

    public CafeBar setAction(@StringRes int res, @Nullable CafeBarCallback callback) {
        String string = mBuilder.mContext.getResources().getString(res);
        int actionColor = CafeBarUtil.getAccentColor(mBuilder.mContext, mBuilder.mTheme.getTitleColor());
        setButtonAction(string, actionColor, callback);
        return this;
    }

    public CafeBar setAction(@NonNull String action, @Nullable CafeBarCallback callback) {
        int actionColor = CafeBarUtil.getAccentColor(mBuilder.mContext, mBuilder.mTheme.getTitleColor());
        setButtonAction(action, actionColor, callback);
        return this;
    }

    public CafeBar setAction(@StringRes int res, int color, @Nullable CafeBarCallback callback) {
        String string = mBuilder.mContext.getResources().getString(res);
        setButtonAction(string, color, callback);
        return this;
    }

    public CafeBar setAction(@NonNull String action, int color, @Nullable CafeBarCallback callback) {
        setButtonAction(action, color, callback);
        return this;
    }

    private void setButtonAction(@NonNull String action, int color, @Nullable final CafeBarCallback callback) {
        if (mBuilder.mCustomView != null) {
            LogUtil.d("CafeBar has customView, setAction ignored.");
            return;
        }

        LogUtil.d("preparing action view");
        mBuilder.mNeutralText = action;
        mBuilder.mNeutralColor = color;

        LinearLayout root = (LinearLayout) getView();
        boolean longAction = CafeBarUtil.isLongAction(action);

        if (root.getChildCount() > 1) {
            LogUtil.d("setAction already set from builder via neutralText");
            return;
        }

        TextView content = root.findViewById(R.id.cafebar_content);

        int side = mBuilder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_content_padding_side);
        int top = mBuilder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_content_padding_top);
        int buttonPadding = mBuilder.mContext.getResources().getDimensionPixelSize(
                R.dimen.cafebar_button_padding);
        int bottom = 0;

        if (longAction) {
            bottom = buttonPadding;
            root.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(0, 0, buttonPadding, 0);
        } else {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) content.getLayoutParams();
            params.width = 0;
            params.weight = 1f;
            content.setLayoutParams(params);
        }

        int navBar = 0;
        if (mBuilder.mFitSystemWindow && !mBuilder.mFloating) {
            navBar = CafeBarUtil.getNavigationBarHeight(mBuilder.mContext);
        }

        Configuration configuration = mBuilder.mContext.getResources().getConfiguration();
        boolean tabletMode = mBuilder.mContext.getResources().getBoolean(R.bool.cafebar_tablet_mode);

        if (tabletMode || configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (mBuilder.mLongContent) {
                LogUtil.d("content has multi lines");
                root.setPadding(side, side, (side - buttonPadding), (side - bottom + navBar));
            } else if (longAction){
                LogUtil.d("content only 1 line with longAction");
                root.setPadding(side, top, (side - buttonPadding), (top - buttonPadding + navBar));
            } else {
                LogUtil.d("content only 1 line");
                root.setPadding(side, (top - buttonPadding), (side - buttonPadding), (top - buttonPadding + navBar));
            }
        } else {
            if (mBuilder.mLongContent) {
                LogUtil.d("content has multi lines");
                root.setPadding(side, side, (side - buttonPadding + navBar), (side - bottom));
            } else if (longAction) {
                LogUtil.d("content only 1 line with longAction");
                root.setPadding(side, top, (side - buttonPadding + navBar), (top - buttonPadding));
            } else {
                LogUtil.d("content only 1 line");
                root.setPadding(side, (top - buttonPadding), (side - buttonPadding + navBar), (top - buttonPadding));
            }
        }

        TextView button = CafeBarUtil.getActionView(mBuilder, action, color);
        if (mBuilder.getTypeface(FONT_NEUTRAL) != null) {
            button.setTypeface(mBuilder.getTypeface(FONT_NEUTRAL));
        }

        if (!longAction) {
            boolean multiLines = CafeBarUtil.isContentMultiLines(mBuilder);
            if (multiLines) {
                if (mBuilder.mFitSystemWindow && !mBuilder.mFloating) {
                    if (tabletMode || configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        root.setPadding(side, side, (side - buttonPadding), (side + navBar));
                    } else {
                        root.setPadding(side, side, (side - buttonPadding + navBar), side);
                    }
                } else {
                    root.setPadding(side, side, (side - buttonPadding), side);
                }
            }
        }

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (callback != null) {
                    callback.OnClick(getCafeBar());
                    return;
                }

                LogUtil.d("callback = null, CafeBar dismissed");
                dismiss();
            }
        });

        root.addView(button);
    }



    @NonNull
    private CafeBar getCafeBar() {
        return this;
    }

    @NonNull
    public View getView() {
        Snackbar.SnackbarLayout snackBarLayout = (Snackbar.SnackbarLayout) mSnackBar.getView();

        boolean tabletMode = mBuilder.mContext.getResources().getBoolean(R.bool.cafebar_tablet_mode);

        if (tabletMode || mBuilder.mFloating) {
            CardView cardView = (CardView) snackBarLayout.getChildAt(0);
            return cardView.getChildAt(0);
        }

        LinearLayout linearLayout = (LinearLayout) snackBarLayout.getChildAt(0);
        if (mBuilder.mShowShadow) return linearLayout.getChildAt(1);
        return linearLayout.getChildAt(0);
    }

    public void dismiss() {
        if (mSnackBar == null) return;

        mSnackBar.dismiss();
    }

    public void show() {
        mSnackBar.show();

        if (mBuilder.mSwipeToDismiss) return;

        if (mSnackBar.getView().getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            mSnackBar.getView().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                @Override
                public boolean onPreDraw() {
                    mSnackBar.getView().getViewTreeObserver().removeOnPreDrawListener(this);
                    ((CoordinatorLayout.LayoutParams) mSnackBar.getView().getLayoutParams()).setBehavior(null);
                    return true;
                }
            });
        }
    }

    @NonNull
    public static Builder builder(@NonNull Context context) {
        return new Builder(context);
    }

    @SuppressWarnings("unused")
    public static class Builder {

        Context mContext;

        @NonNull View mTo;
        @Nullable View mCustomView;
        CafeBarTheme.Custom mTheme = CafeBarTheme.Custom(CafeBarTheme.DARK.getColor());
        CafeBarGravity mGravity = CafeBarGravity.CENTER;

        @Snackbar.Duration
        int mDuration = Snackbar.LENGTH_SHORT;
        int mMaxLines = 2;
        @ColorInt
        int mPositiveColor = mTheme.getTitleColor();
        @ColorInt int mNegativeColor = mTheme.getTitleColor();
        @ColorInt int mNeutralColor = mTheme.getTitleColor();

        boolean mLongContent = false;
        boolean mAutoDismiss = true;
        boolean mShowShadow = true;
        boolean mFitSystemWindow = false;
        boolean mFloating = false;
        boolean mTintIcon = true;
        boolean mSwipeToDismiss = true;

        private HashMap<String, WeakReference<Typeface>> mTypefaces;

        @Nullable Drawable mIcon = null;

        String mContent = "";
        @Nullable String mPositiveText = null;
        @Nullable String mNegativeText = null;
        @Nullable String mNeutralText = null;

        @Nullable SpannableStringBuilder mSpannableBuilder = null;

        @Nullable CafeBarCallback mPositiveCallback;
        @Nullable CafeBarCallback mNegativeCallback;
        @Nullable CafeBarCallback mNeutralCallback;

        public Builder(@NonNull Context context) {
            mContext = context;
            mTypefaces = new HashMap<>();

            mTo = ((Activity) mContext).getWindow().getDecorView()
                    .findViewById(android.R.id.content);
        }

        public Builder to(@Nullable View view) {
            if (view != null) {
                mTo = view;
                return this;
            }

            LogUtil.e("to(View): view is null, ignored");
            return this;
        }

        public Builder customView(@LayoutRes int res) {
            View view = View.inflate(mContext, res, null);
            return customView(view);
        }

        public Builder customView(@Nullable View customView) {
            mCustomView = customView;
            return this;
        }

        public Builder content(@StringRes int res) {
            return content(mContext.getResources().getString(res));
        }

        public Builder content(@NonNull String content) {
            mContent = content;
            return this;
        }

        public Builder content(@NonNull SpannableStringBuilder spannableBuilder) {
            mSpannableBuilder = spannableBuilder;
            return this;
        }

        public Builder maxLines(@IntRange(from = 1, to = 6) int maxLines) {
            mMaxLines = maxLines;
            return this;
        }

        public Builder duration(@Snackbar.Duration int duration) {
            mDuration = duration;
            return this;
        }

        public Builder theme(@NonNull CafeBarTheme theme) {
            return theme(CafeBarTheme.Custom(theme.getColor()));
        }

        public Builder theme(@NonNull CafeBarTheme.Custom customTheme) {
            mTheme = customTheme;
            mPositiveColor = mTheme.getTitleColor();
            mNegativeColor = mNeutralColor = mTheme.getSubTitleColor();
            return this;
        }

        public Builder icon(@Nullable Bitmap icon) {
            return icon(CafeBarUtil.toDrawable(mContext, icon), true);
        }

        public Builder icon(@DrawableRes int res) {
            return icon(CafeBarUtil.getDrawable(mContext, res), true);
        }

        public Builder icon(@Nullable Drawable icon) {
            return icon(icon, true);
        }

        public Builder icon(@Nullable Bitmap icon, boolean tintIcon) {
            return icon(CafeBarUtil.toDrawable(mContext, icon), tintIcon);
        }

        public Builder icon(@DrawableRes int res, boolean tintIcon) {
            return icon(CafeBarUtil.getDrawable(mContext, res), tintIcon);
        }

        public Builder icon(@Nullable Drawable icon, boolean tintIcon) {
            mIcon = icon;
            mTintIcon = tintIcon;
            return this;
        }

        public Builder showShadow(@BoolRes int res) {
            return showShadow(mContext.getResources().getBoolean(res));
        }

        public Builder showShadow(boolean showShadow) {
            mShowShadow = showShadow;
            return this;
        }

        public Builder autoDismiss(@BoolRes int res) {
            return autoDismiss(mContext.getResources().getBoolean(res));
        }

        public Builder autoDismiss(boolean autoDismiss) {
            mAutoDismiss = autoDismiss;
            return this;
        }

        public Builder swipeToDismiss(@BoolRes int res) {
            return swipeToDismiss(mContext.getResources().getBoolean(res));
        }

        public Builder swipeToDismiss(boolean swipeToDismiss) {
            mSwipeToDismiss = swipeToDismiss;
            return this;
        }

        public Builder floating(@BoolRes int res) {
            return floating(mContext.getResources().getBoolean(res));
        }

        public Builder floating(boolean floating) {
            mFloating = floating;
            return this;
        }

        public Builder gravity(@NonNull CafeBarGravity gravity) {
            mGravity = gravity;
            return this;
        }

        public Builder fitSystemWindow() {
            Activity activity = (Activity) mContext;
            Window window = activity.getWindow();
            if (window == null) {
                LogUtil.d("fitSystemWindow() window is null");
                return this;
            }

            WindowManager.LayoutParams params = window.getAttributes();
            int navigationBarHeight = CafeBarUtil.getNavigationBarHeight(mContext);

            boolean isInMultiWindowMode = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                isInMultiWindowMode = activity.isInMultiWindowMode();
            }

            if ((params.flags & WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) ==
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) {
                mFitSystemWindow = navigationBarHeight > 0 && !isInMultiWindowMode;
            }
            return this;
        }

        public Builder typeface(String contentFontName, String buttonFontName) {
            return typeface(CafeBarUtil.getTypeface(mContext, contentFontName),
                    CafeBarUtil.getTypeface(mContext, buttonFontName));
        }

        public Builder typeface(@Nullable Typeface content, @Nullable Typeface button) {
            addTypeface(FONT_CONTENT, content);
            addTypeface(FONT_POSITIVE, button);
            addTypeface(FONT_NEGATIVE, button);
            addTypeface(FONT_NEUTRAL, button);
            return this;
        }

        public Builder contentTypeface(String fontName) {
            return contentTypeface(CafeBarUtil.getTypeface(mContext, fontName));
        }

        public Builder contentTypeface(@Nullable Typeface typeface) {
            addTypeface(FONT_CONTENT, typeface);
            return this;
        }

        public Builder positiveTypeface(String fontName) {
            return positiveTypeface(CafeBarUtil.getTypeface(mContext, fontName));
        }

        public Builder positiveTypeface(@Nullable Typeface typeface) {
            addTypeface(FONT_POSITIVE,typeface);
            return this;
        }

        public Builder negativeTypeface(String fontName) {
            return negativeTypeface(CafeBarUtil.getTypeface(mContext, fontName));
        }

        public Builder negativeTypeface(@Nullable Typeface typeface) {
            addTypeface(FONT_NEGATIVE, typeface);
            return this;
        }

        public Builder neutralTypeface(String fontName) {
            return neutralTypeface(CafeBarUtil.getTypeface(mContext, fontName));
        }

        public Builder neutralTypeface(@Nullable Typeface typeface) {
            addTypeface(FONT_NEUTRAL, typeface);
            return this;
        }

        public Builder buttonTypeface(String fontName) {
            return buttonTypeface(CafeBarUtil.getTypeface(mContext, fontName));
        }

        public Builder buttonTypeface(@Nullable Typeface typeface) {
            addTypeface(FONT_POSITIVE, typeface);
            addTypeface(FONT_NEGATIVE, typeface);
            addTypeface(FONT_NEUTRAL, typeface);
            return this;
        }

        public Builder positiveColor(int positiveColor) {
            mPositiveColor = CafeBarUtil.getColor(mContext, positiveColor);
            return this;
        }

        public Builder negativeColor(int negativeColor) {
            mNegativeColor = CafeBarUtil.getColor(mContext, negativeColor);
            return this;
        }

        public Builder neutralColor(int neutralColor) {
            mNeutralColor = CafeBarUtil.getColor(mContext, neutralColor);
            return this;
        }

        public Builder buttonColor(int buttonColor) {
            int color = CafeBarUtil.getColor(mContext, buttonColor);
            mNeutralColor = mPositiveColor = mNegativeColor = color;
            return this;
        }

        public Builder positiveText(@StringRes int res) {
            return positiveText(mContext.getResources().getString(res));
        }

        public Builder positiveText(@NonNull String positiveText) {
            mPositiveText = positiveText;
            return this;
        }

        public Builder negativeText(@StringRes int res) {
            return negativeText(mContext.getResources().getString(res));
        }

        public Builder negativeText(@NonNull String negativeText) {
            mNegativeText = negativeText;
            return this;
        }

        public Builder neutralText(@StringRes int res) {
            return neutralText(mContext.getResources().getString(res));
        }

        public Builder neutralText(@NonNull String neutralText) {
            mNeutralText = neutralText;
            return this;
        }

        public Builder onPositive(@Nullable CafeBarCallback positiveCallback) {
            mPositiveCallback = positiveCallback;
            return this;
        }

        public Builder onNegative(@Nullable CafeBarCallback negativeCallback) {
            mNegativeCallback = negativeCallback;
            return this;
        }

        public Builder onNeutral(@Nullable CafeBarCallback neutralCallback) {
            mNeutralCallback = neutralCallback;
            return this;
        }

        public CafeBar build() {
            return new CafeBar(this);
        }

        public void show() {
            build().show();
        }

        private void addTypeface(String name, Typeface typeface) {
            if (!mTypefaces.containsKey(name) || mTypefaces.get(name) == null) {
                mTypefaces.put(name, new WeakReference<>(typeface));
            }
        }

        @Nullable
        Typeface getTypeface(String name) {
            if (mTypefaces.get(name) != null) {
                return mTypefaces.get(name).get();
            }
            return null;
        }
    }
}
