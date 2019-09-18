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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

class CafeBarUtil {

    @NonNull
    static View getBaseCafeBarView(@NonNull CafeBar.Builder builder) {
        int color = builder.mTheme.getColor();
        int titleColor = builder.mTheme.getTitleColor();

        //Creating LinearLayout as rootView
        LinearLayout root = new LinearLayout(builder.mContext);
        root.setId(R.id.cafebar_root);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackgroundColor(color);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.setClickable(true);

        //Creating TextView for content
        TextView content = new TextView(builder.mContext);
        content.setId(R.id.cafebar_content);
        content.setMaxLines(builder.mMaxLines);
        content.setEllipsize(TextUtils.TruncateAt.END);
        content.setTextColor(titleColor);
        content.setTextSize(TypedValue.COMPLEX_UNIT_PX, builder.mContext.getResources()
                    .getDimensionPixelSize(R.dimen.cafebar_content_text));
        if (builder.getTypeface(CafeBar.FONT_CONTENT) != null) {
            content.setTypeface(builder.getTypeface(CafeBar.FONT_CONTENT));
        }

        content.setText(builder.mContent);
        if (builder.mSpannableBuilder != null) {
            content.setText(builder.mSpannableBuilder, TextView.BufferType.SPANNABLE);
        }

        content.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        content.setGravity(Gravity.CENTER_VERTICAL);

        boolean tabletMode = builder.mContext.getResources().getBoolean(R.bool.cafebar_tablet_mode);
        if (tabletMode || builder.mFloating) {
            content.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            content.setMinWidth(builder.mContext.getResources()
                    .getDimensionPixelSize(R.dimen.cafebar_floating_min_width));
            content.setMaxWidth(builder.mContext.getResources()
                    .getDimensionPixelSize(R.dimen.cafebar_floating_max_width));
        }

        int side = builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_content_padding_side);
        int top = builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_content_padding_top);

        if (builder.mIcon != null) {
            Drawable drawable = getResizedDrawable(
                    builder.mContext,
                    builder.mIcon,
                    titleColor,
                    builder.mTintIcon);
            if (drawable != null) {
                content.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                content.setCompoundDrawablePadding(top);
            }
        }

        boolean multiLines = isContentMultiLines(builder);
        boolean containsPositive = builder.mPositiveText != null;
        boolean containsNegative = builder.mNegativeText != null;
        boolean longNeutralAction = isLongAction(builder.mNeutralText);

        if (multiLines || containsPositive || containsNegative || longNeutralAction) {
            top = side;
            builder.mLongContent = true;
        }

        root.setPadding(side, top, side, top);

        if (builder.mPositiveText == null && builder.mNegativeText == null) {
            if (builder.mFitSystemWindow && !builder.mFloating) {
                Configuration configuration = builder.mContext.getResources().getConfiguration();
                int navBar = getNavigationBarHeight(builder.mContext);

                if (tabletMode || configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    root.setPadding(side, top, side, (top + navBar));
                } else {
                    root.setPadding(side, top, (side + navBar), top);
                }
            }

            //Adding childView to rootView
            root.addView(content);

            //Returning rootView
            return root;
        }

        //Change root orientation to vertical
        root.setOrientation(LinearLayout.VERTICAL);

        //Creating another linear layout for button container
        LinearLayout buttonBase = new LinearLayout(builder.mContext);
        buttonBase.setId(R.id.cafebar_button_base);
        buttonBase.setOrientation(LinearLayout.HORIZONTAL);
        buttonBase.setGravity(Gravity.END);
        buttonBase.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        //Adding button
        String neutralText = builder.mNeutralText;
        if (neutralText != null) {
            TextView neutral = getActionView(builder, neutralText, builder.mNeutralColor);
            neutral.setId(R.id.cafebar_button_neutral);

            if (builder.getTypeface(CafeBar.FONT_NEUTRAL) != null) {
                neutral.setTypeface(builder.getTypeface(CafeBar.FONT_NEUTRAL));
            }

            buttonBase.addView(neutral);
        }

        String negativeText = builder.mNegativeText;
        if (negativeText != null) {
            TextView negative = getActionView(builder, negativeText, builder.mNegativeColor);
            negative.setId(R.id.cafebar_button_negative);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) negative.getLayoutParams();
            params.setMargins(
                    params.leftMargin + builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_button_margin),
                    params.topMargin,
                    params.rightMargin,
                    params.bottomMargin);

            if (builder.getTypeface(CafeBar.FONT_NEGATIVE) != null) {
                negative.setTypeface(builder.getTypeface(CafeBar.FONT_NEGATIVE));
            }

            buttonBase.addView(negative);
        }

        String positiveText = builder.mPositiveText;
        if (positiveText != null) {
            int positiveColor = CafeBarUtil.getAccentColor(builder.mContext, builder.mPositiveColor);
            TextView positive = getActionView(builder, positiveText, positiveColor);
            positive.setId(R.id.cafebar_button_positive);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) positive.getLayoutParams();
            params.setMargins(
                    params.leftMargin + builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_button_margin),
                    params.topMargin,
                    params.rightMargin,
                    params.bottomMargin);

            if (builder.getTypeface(CafeBar.FONT_POSITIVE) != null) {
                positive.setTypeface(builder.getTypeface(CafeBar.FONT_POSITIVE));
            }

            buttonBase.addView(positive);
        }

        //Adjust padding
        int buttonPadding = builder.mContext.getResources().getDimensionPixelSize(
                R.dimen.cafebar_button_padding);
        root.setPadding(side, top, (side - buttonPadding), (top - buttonPadding));

        if (builder.mFitSystemWindow && !builder.mFloating) {
            Configuration configuration = builder.mContext.getResources().getConfiguration();
            int navBar = getNavigationBarHeight(builder.mContext);

            if (tabletMode || configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                root.setPadding(side, top, (side - buttonPadding), (top - buttonPadding + navBar));
            } else {
                root.setPadding(side, top, (side - buttonPadding + navBar), top);
            }
        }

        //Adding content to container
        content.setPadding(0, 0, buttonPadding, 0);

        //Adding childView to rootView
        root.addView(content);

        //Adding button container to root
        root.addView(buttonBase);

        //Returning rootView
        return root;
    }

    static Snackbar getBaseSnackBar(@NonNull View cafeBarLayout,
                                    @NonNull CafeBar.Builder builder) {
        View view = builder.mTo;

        Snackbar snackBar = Snackbar.make(view, "", builder.mAutoDismiss ?
                builder.mDuration : Snackbar.LENGTH_INDEFINITE);
        Snackbar.SnackbarLayout snackBarLayout = (Snackbar.SnackbarLayout) snackBar.getView();
        snackBarLayout.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        snackBarLayout.setPadding(0, 0, 0, 0);
        snackBarLayout.setBackgroundColor(Color.TRANSPARENT);
        snackBarLayout.setClickable(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            snackBarLayout.setElevation(0);
        }

        TextView textView = snackBarLayout.findViewById(
                R.id.snackbar_text);
        if (textView != null) textView.setVisibility(View.INVISIBLE);

        boolean tabletMode = builder.mContext.getResources().getBoolean(R.bool.cafebar_tablet_mode);
        if (tabletMode || builder.mFloating) {
            int shadow = builder.mContext.getResources().getDimensionPixelSize(R.dimen.cardview_default_elevation);
            int padding = builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_floating_padding);

            CardView cardView = new CardView(builder.mContext);
            cardView.setUseCompatPadding(false);
            Snackbar.SnackbarLayout.LayoutParams params = new Snackbar.SnackbarLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = builder.mGravity.getGravity();

            int bottom = builder.mFloating ? padding : 0;
            snackBarLayout.setClipToPadding(false);
            snackBarLayout.setPadding(padding, shadow, padding, bottom);

            if (builder.mFitSystemWindow && builder.mFloating) {
                Configuration configuration = builder.mContext.getResources().getConfiguration();
                int navBar = getNavigationBarHeight(builder.mContext);

                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    snackBarLayout.setPadding(padding, shadow, padding, bottom + navBar);
                } else {
                    snackBarLayout.setPadding(padding, shadow, padding + navBar, bottom);
                }
            }

            cardView.setLayoutParams(params);
            cardView.setClickable(true);
            if (!builder.mShowShadow) {
                cardView.setCardElevation(0f);
            }

            cardView.addView(cafeBarLayout);
            snackBarLayout.addView(cardView, 0);
            return snackBar;
        }

        LinearLayout root = new LinearLayout(builder.mContext);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        if (builder.mShowShadow) {
            View shadow = new View(builder.mContext);
            shadow.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    builder.mContext.getResources().getDimensionPixelSize(
                            R.dimen.cafebar_shadow_top)));
            shadow.setBackgroundResource(R.drawable.cafebar_shadow_top);
            root.addView(shadow);
        }

        root.addView(cafeBarLayout);
        snackBarLayout.addView(root, 0);
        return snackBar;
    }

    @NonNull
    static TextView getActionView(@NonNull CafeBar.Builder builder, @NonNull String action, int color) {
        boolean longAction = isLongAction(action);
        int res = R.layout.cafebar_action_button_dark;

        CafeBarTheme.Custom customTheme = builder.mTheme;
        int titleColor = customTheme.getTitleColor();
        boolean dark = titleColor != Color.WHITE;
        if (dark) {
            res = R.layout.cafebar_action_button;
        }

        int padding = builder.mContext.getResources().getDimensionPixelSize(
                R.dimen.cafebar_button_padding);

        TextView button = (TextView) View.inflate(builder.mContext, res, null);
        button.setText(action.toUpperCase(Locale.getDefault()));
        button.setMaxLines(1);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setTextColor(color);
        button.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        int side = builder.mContext.getResources().getDimensionPixelSize(
                R.dimen.cafebar_content_padding_side);
        int margin = builder.mContext.getResources().getDimensionPixelSize(
                R.dimen.cafebar_button_margin_start);
        params.setMargins(margin, 0, 0, 0);

        if (longAction) {
            params.setMargins(0, (side - padding), 0, 0);
        }

        if (builder.mPositiveText != null || builder.mNegativeText != null) {
            longAction = true;
            params.setMargins(0, (side - padding), 0, 0);
        } else {
            params.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        }

        button.setLayoutParams(params);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            button.setBackgroundResource(dark ? R.drawable.cafebar_action_button_selector_dark :
                    R.drawable.cafebar_action_button_selector);
            return button;
        }

        TypedValue outValue = new TypedValue();
        builder.mContext.getTheme().resolveAttribute(longAction ?
                        R.attr.selectableItemBackground : R.attr.selectableItemBackgroundBorderless,
                outValue, true);
        button.setBackgroundResource(outValue.resourceId);
        return button;
    }

    static boolean isContentMultiLines(@NonNull CafeBar.Builder builder) {
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) builder.mContext).getWindowManager().getDefaultDisplay().getMetrics(metrics);

        boolean tabletMode = builder.mContext.getResources().getBoolean(R.bool.cafebar_tablet_mode);
        int padding = (builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_content_padding_side) * 2);

        if (builder.mNeutralText != null && builder.mNegativeText == null && builder.mPositiveText == null &&
                !isLongAction(builder.mNeutralText)) {
            padding += builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_button_margin_start);

            int actionPadding = builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_button_padding);
            TextView action = new TextView(builder.mContext);
            action.setTextSize(TypedValue.COMPLEX_UNIT_PX, builder.mContext.getResources()
                    .getDimensionPixelSize(R.dimen.cafebar_content_text));
            if (builder.getTypeface(CafeBar.FONT_NEUTRAL) != null) {
                action.setTypeface(builder.getTypeface(CafeBar.FONT_CONTENT));
            }
            action.setPadding(actionPadding, 0, actionPadding, 0);
            action.setText(builder.mNeutralText.substring(0,
                    builder.mNeutralText.length() > 10 ? 10 : builder.mNeutralText.length()));

            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.AT_MOST);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            action.measure(widthMeasureSpec, heightMeasureSpec);

            LogUtil.d("measured action width: " +action.getMeasuredWidth());
            padding += action.getMeasuredWidth();
        }

        if (builder.mIcon != null) {
            int icon = builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_icon_size);
            icon += builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_content_padding_top);
            padding += icon;
        }

        TextView textView = new TextView(builder.mContext);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, builder.mContext.getResources()
                .getDimension(R.dimen.cafebar_content_text));
        textView.setPadding(padding, 0, 0, 0);
        if (builder.getTypeface(CafeBar.FONT_CONTENT) != null) {
            textView.setTypeface(builder.getTypeface(CafeBar.FONT_CONTENT));
        }

        if (builder.mSpannableBuilder != null) {
            textView.setText(builder.mSpannableBuilder, TextView.BufferType.SPANNABLE);
        } else {
            textView.setText(builder.mContent);
        }

        int maxWidth = metrics.widthPixels;
        if (builder.mFloating || tabletMode) {
            maxWidth = builder.mContext.getResources().getDimensionPixelSize(R.dimen.cafebar_floating_max_width);
        }

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        textView.measure(widthMeasureSpec, heightMeasureSpec);
        return textView.getLineCount() > 1;
    }

    @Nullable
    static Drawable getDrawable(@NonNull Context context, @DrawableRes int res) {
        try {
            Drawable drawable = context.getResources().getDrawable(res);
            return drawable.mutate();
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    @Nullable
    static Drawable toDrawable(@NonNull Context context, @Nullable Bitmap bitmap) {
        try {
            if (bitmap == null) return null;
            return new BitmapDrawable(context.getResources(), bitmap);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    @Nullable
    private static Drawable getResizedDrawable(@NonNull Context context, @Nullable Drawable drawable,
                                               int color, boolean tint) {
        try {
            if (drawable == null) {
                LogUtil.d("drawable: null");
                return null;
            }

            if (tint) {
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                drawable.mutate();
            }

            int size = context.getResources().getDimensionPixelSize(R.dimen.cafebar_icon_size);
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return new BitmapDrawable(context.getResources(),
                    Bitmap.createScaledBitmap(bitmap, size, size, true));
        } catch (Exception | OutOfMemoryError e) {
            LogUtil.e(Log.getStackTraceString(e));
            return null;
        }
    }

    static int getNavigationBarHeight(@NonNull Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize = getRealScreenSize(context);

        if (appUsableSize.x < realScreenSize.x) {
            Point point = new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
            return point.x;
        }

        if (appUsableSize.y < realScreenSize.y) {
            Point point = new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
            return point.y;
        }
        return 0;
    }

    private static Point getAppUsableScreenSize(@NonNull Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    private static Point getRealScreenSize(@NonNull Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                size.x = (Integer)     Display.class.getMethod("getRawWidth").invoke(display);
                size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (Exception e) {
                LogUtil.e(Log.getStackTraceString(e));
            }
        }
        return size;
    }

    static boolean isLongAction(@Nullable String action) {
        return action != null && action.length() > 10;
    }

    static int getAccentColor(Context context, int defaultColor) {
        if (context == null) {
            LogUtil.e("getAccentColor() context is null");
            return defaultColor;
        }

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        return typedValue.data;
    }

    static int getTitleTextColor(@ColorInt int color) {
        double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;
        return (darkness < 0.35) ? getDarkerColor(color) : Color.WHITE;
    }

    static int getSubTitleTextColor(@ColorInt int color) {
        int titleColor = getTitleTextColor(color);
        int alpha2 = Math.round(Color.alpha(titleColor) * 0.7f);
        int red = Color.red(titleColor);
        int green = Color.green(titleColor);
        int blue = Color.blue(titleColor);
        return Color.argb(alpha2, red, green, blue);
    }

    private static int getDarkerColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.25f;
        return Color.HSVToColor(hsv);
    }

    static int getColor(@NonNull Context context, int color) {
        try {
            return ContextCompat.getColor(context, color);
        } catch (Exception e) {
            LogUtil.e(Log.getStackTraceString(e));
            return color;
        }
    }

    @Nullable
    static Typeface getTypeface(@NonNull Context context, String fontName) {
        try {
            return Typeface.createFromAsset(context.getAssets(), "fonts/" +fontName);
        } catch (Exception e) {
            LogUtil.e(Log.getStackTraceString(e));
        }
        return null;
    }
}
