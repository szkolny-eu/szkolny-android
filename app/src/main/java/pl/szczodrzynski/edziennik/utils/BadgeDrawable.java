package pl.szczodrzynski.edziennik.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public class BadgeDrawable extends Drawable {

    private float mTextSize;
    private Paint mBadgePaint;
    private Paint mTextPaint;
    private Rect mTxtRect = new Rect();

    private String mCount = "";
    private boolean mTextEnabled = true;
    private boolean mWillDraw = false;


    public BadgeDrawable(Context context) {
        mTextSize = dpToPx(context, 11); //text size
        mBadgePaint = new Paint();
        mBadgePaint.setColor(Color.RED);
        mBadgePaint.setAntiAlias(true);
        mBadgePaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(Typeface.DEFAULT);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    private float dpToPx(Context context, float value) {
        Resources r = context.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, r.getDisplayMetrics());
        return px;
    }


    @Override
    public void draw(Canvas canvas) {
        if (!mWillDraw) {
            return;
        }
        Rect bounds = getBounds();
        float width = bounds.right - bounds.left;
        float height = bounds.bottom - bounds.top;
        // Position the badge in the top-right quadrant of the icon.

        /*Using Math.max rather than Math.min */
//        float radius = ((Math.max(width, height) / 2)) / 2;
        float radius = width * 0.15f;
        float centerX = (width - radius - 1) +10;
        float centerY = radius -5;
        if (mTextEnabled) {
            if(mCount.length() <= 2){
                // Draw badge circle.
                canvas.drawCircle(centerX, centerY, radius+13, mBadgePaint);
            }
            else{
                canvas.drawCircle(centerX, centerY, radius+16, mBadgePaint);
            }
            // Draw badge count text inside the circle.
            mTextPaint.getTextBounds(mCount, 0, mCount.length(), mTxtRect);
            float textHeight = mTxtRect.bottom - mTxtRect.top;
            float textY = centerY + (textHeight / 2f);
            if (mCount.length() > 2)
                canvas.drawText("99+", centerX, textY, mTextPaint);
            else
                canvas.drawText(mCount, centerX, textY, mTextPaint);
        }
        else {
            canvas.drawCircle(centerX, centerY, radius+5, mBadgePaint);
        }
    }

    /*
     Sets the count (i.e notifications) to display.
      */
    public void setCount(String count) {
        mCount = count;
        // Only draw a badge if there are notifications.
        mWillDraw = !count.equalsIgnoreCase("0");
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        // do nothing
    }

    public void setTextEnabled(boolean mTextEnabled) {
        this.mTextEnabled = mTextEnabled;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // do nothing
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }
}
