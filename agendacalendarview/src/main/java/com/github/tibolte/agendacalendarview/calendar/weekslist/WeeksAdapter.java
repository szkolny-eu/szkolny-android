package com.github.tibolte.agendacalendarview.calendar.weekslist;

import com.github.tibolte.agendacalendarview.CalendarManager;
import com.github.tibolte.agendacalendarview.R;
import com.github.tibolte.agendacalendarview.models.CalendarEvent;
import com.github.tibolte.agendacalendarview.models.IDayItem;
import com.github.tibolte.agendacalendarview.models.IWeekItem;
import com.github.tibolte.agendacalendarview.utils.BusProvider;
import com.github.tibolte.agendacalendarview.utils.DateHelper;
import com.github.tibolte.agendacalendarview.utils.Events;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WeeksAdapter extends RecyclerView.Adapter<WeeksAdapter.WeekViewHolder> {

    public static final long FADE_DURATION = 250;

    private Context mContext;
    private Calendar mToday;
    private List<IWeekItem> mWeeksList = new ArrayList<>();
    private List<CalendarEvent> mEventList = new ArrayList<>();
    private boolean mDragging;
    private boolean mAlphaSet;
    private int mDayTextColor, mPastDayTextColor, mCurrentDayColor;

    // region Constructor

    public WeeksAdapter(Context context, Calendar today, int dayTextColor, int currentDayTextColor, int pastDayTextColor, List<CalendarEvent> events) {
        this.mToday = today;
        this.mContext = context;
        this.mDayTextColor = dayTextColor;
        this.mCurrentDayColor = currentDayTextColor;
        this.mPastDayTextColor = pastDayTextColor;
        this.mEventList = events;
    }

    // endregion

    public void updateWeeksItems(List<IWeekItem> weekItems) {
        this.mWeeksList.clear();
        this.mWeeksList.addAll(weekItems);
        notifyDataSetChanged();
    }

    // region Getters/setters

    public List<IWeekItem> getWeeksList() {
        return mWeeksList;
    }

    public boolean isDragging() {
        return mDragging;
    }

    public void setDragging(boolean dragging) {
        if (dragging != this.mDragging) {
            this.mDragging = dragging;
            notifyItemRangeChanged(0, mWeeksList.size());
        }
    }

    public boolean isAlphaSet() {
        return mAlphaSet;
    }

    public void setAlphaSet(boolean alphaSet) {
        mAlphaSet = alphaSet;
    }

    // endregion

    // region RecyclerView.Adapter<WeeksAdapter.WeekViewHolder> methods

    @Override
    public WeekViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_week, parent, false);
        return new WeekViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WeekViewHolder weekViewHolder, int position) {
        IWeekItem weekItem = mWeeksList.get(position);
        weekViewHolder.bindWeek(weekItem, mToday);
    }

    @Override
    public int getItemCount() {
        return mWeeksList.size();
    }

    // endregion

    // region Class - WeekViewHolder

    public class WeekViewHolder extends RecyclerView.ViewHolder {

        /**
         * List of layout containers for each day
         */
        private List<LinearLayout> mCells;
        private TextView mTxtMonth;
        private FrameLayout mMonthBackground;

        public WeekViewHolder(View itemView) {
            super(itemView);
            mTxtMonth = (TextView) itemView.findViewById(R.id.month_label);
            mMonthBackground = (FrameLayout) itemView.findViewById(R.id.month_background);
            LinearLayout daysContainer = (LinearLayout) itemView.findViewById(R.id.week_days_container);
            setUpChildren(daysContainer);
        }

        public void bindWeek(IWeekItem weekItem, Calendar today) {
            setUpMonthOverlay();

            List<IDayItem> dayItems = weekItem.getDayItems();

            for (int c = 0; c < dayItems.size(); c++) {
                final IDayItem dayItem = dayItems.get(c);
                LinearLayout cellItem = mCells.get(c);
                TextView txtDay = (TextView) cellItem.findViewById(R.id.view_day_day_label);
                TextView txtMonth = (TextView) cellItem.findViewById(R.id.view_day_month_label);
                View circleView = cellItem.findViewById(R.id.view_day_circle_selected);
                View eventIndicator1 = cellItem.findViewById(R.id.view_day_event_indicator1);
                View eventIndicator2 = cellItem.findViewById(R.id.view_day_event_indicator2);
                View eventIndicator3 = cellItem.findViewById(R.id.view_day_event_indicator3);
                cellItem.setOnClickListener(v->BusProvider.getInstance().send(new Events.DayClickedEvent(dayItem)));

                eventIndicator1.setVisibility(View.INVISIBLE);
                eventIndicator2.setVisibility(View.INVISIBLE);
                eventIndicator3.setVisibility(View.INVISIBLE);

                Calendar dayItemCalendar = Calendar.getInstance();
                dayItemCalendar.setTime(dayItem.getDate());
                int eventCount = 0;
                for (CalendarEvent event: mEventList) {
                    if (event.getStartTime().get(Calendar.YEAR) == dayItemCalendar.get(Calendar.YEAR)
                            && event.getStartTime().get(Calendar.MONTH) == dayItemCalendar.get(Calendar.MONTH)
                            && event.getStartTime().get(Calendar.DAY_OF_MONTH) == dayItemCalendar.get(Calendar.DAY_OF_MONTH)) {
                        eventCount++;
                        if (eventCount == 1) {
                            eventIndicator1.setVisibility(View.VISIBLE);
                            eventIndicator1.getBackground().setColorFilter(new PorterDuffColorFilter(event.getColor(),PorterDuff.Mode.MULTIPLY));
                        }
                        if (eventCount == 2) {
                            eventIndicator2.setVisibility(View.VISIBLE);
                            eventIndicator2.getBackground().setColorFilter(new PorterDuffColorFilter(event.getColor(),PorterDuff.Mode.MULTIPLY));
                        }
                        if (eventCount == 3) {
                            eventIndicator3.setVisibility(View.VISIBLE);
                            eventIndicator3.getBackground().setColorFilter(new PorterDuffColorFilter(event.getColor(),PorterDuff.Mode.MULTIPLY));
                        }
                    }
                }

                //Log.d("CalendarView", "Event count for day "+dayItem.getValue()+" is "+eventCount);

                txtMonth.setVisibility(View.GONE);
                txtDay.setTextColor(mDayTextColor);
                txtMonth.setTextColor(mDayTextColor);
                circleView.setVisibility(View.GONE);

                txtDay.setTypeface(null, Typeface.NORMAL);
                txtMonth.setTypeface(null, Typeface.NORMAL);

                // Display the day
                txtDay.setText(Integer.toString(dayItem.getValue()));

                // Highlight first day of the month
                if (dayItem.isFirstDayOfTheMonth() && !dayItem.isSelected()) {
                    txtMonth.setVisibility(View.VISIBLE);
                    txtMonth.setText(dayItem.getMonth());
                    txtDay.setTypeface(null, Typeface.BOLD);
                    txtMonth.setTypeface(null, Typeface.BOLD);
                }

                // Check if this day is in the past
                if (today.getTime().after(dayItem.getDate()) && !DateHelper.sameDate(today, dayItem.getDate())) {
                    txtDay.setTextColor(mPastDayTextColor);
                    txtMonth.setTextColor(mPastDayTextColor);
                }

                // Highlight the cell if this day is today
                if (dayItem.isToday() && !dayItem.isSelected()) {
                    txtDay.setTextColor(mCurrentDayColor);
                }

                if (dayItem.getShowBadge()) {
                    circleView.setVisibility(View.VISIBLE);
                    GradientDrawable drawable = (GradientDrawable) circleView.getBackground();
                    drawable.setStroke((int) (2 * Resources.getSystem().getDisplayMetrics().density), 0xffff0000);
                }

                // Show a circle if the day is selected
                if (dayItem.isSelected()) {
                    txtDay.setTextColor(mDayTextColor);
                    circleView.setVisibility(View.VISIBLE);
                    GradientDrawable drawable = (GradientDrawable) circleView.getBackground();
                    drawable.setStroke((int) (1 * Resources.getSystem().getDisplayMetrics().density), mDayTextColor);
                }

                // Check if the month label has to be displayed
                if (dayItem.getValue() == 15) {
                    mTxtMonth.setVisibility(View.VISIBLE);
                    SimpleDateFormat monthDateFormat = new SimpleDateFormat(mContext.getResources().getString(R.string.month_name_format), CalendarManager.getInstance().getLocale());
                    String month = monthDateFormat.format(weekItem.getDate()).toUpperCase();
                    if (today.get(Calendar.YEAR) != weekItem.getYear()) {
                        month = month + String.format(" %d", weekItem.getYear());
                    }
                    mTxtMonth.setText(month);
                }
            }
        }

        private void setUpChildren(LinearLayout daysContainer) {
            mCells = new ArrayList<>();
            for (int i = 0; i < daysContainer.getChildCount(); i++) {
                mCells.add((LinearLayout) daysContainer.getChildAt(i));
            }
        }

        private void setUpMonthOverlay() {
            mTxtMonth.setVisibility(View.GONE);

            if (isDragging()) {
                AnimatorSet animatorSetFadeIn = new AnimatorSet();
                animatorSetFadeIn.setDuration(FADE_DURATION);
                ObjectAnimator animatorTxtAlphaIn = ObjectAnimator.ofFloat(mTxtMonth, "alpha", mTxtMonth.getAlpha(), 1f);
                ObjectAnimator animatorBackgroundAlphaIn = ObjectAnimator.ofFloat(mMonthBackground, "alpha", mMonthBackground.getAlpha(), 1f);
                animatorSetFadeIn.playTogether(
                        animatorTxtAlphaIn
                        //animatorBackgroundAlphaIn
                );
                animatorSetFadeIn.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setAlphaSet(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animatorSetFadeIn.start();
            } else {
                AnimatorSet animatorSetFadeOut = new AnimatorSet();
                animatorSetFadeOut.setDuration(FADE_DURATION);
                ObjectAnimator animatorTxtAlphaOut = ObjectAnimator.ofFloat(mTxtMonth, "alpha", mTxtMonth.getAlpha(), 0f);
                ObjectAnimator animatorBackgroundAlphaOut = ObjectAnimator.ofFloat(mMonthBackground, "alpha", mMonthBackground.getAlpha(), 0f);
                animatorSetFadeOut.playTogether(
                        animatorTxtAlphaOut
                        //animatorBackgroundAlphaOut
                );
                animatorSetFadeOut.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setAlphaSet(false);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animatorSetFadeOut.start();
            }

            if (isAlphaSet()) {
                //mMonthBackground.setAlpha(1f);
                mTxtMonth.setAlpha(1f);
            } else {
                //mMonthBackground.setAlpha(0f);
                mTxtMonth.setAlpha(0f);
            }
        }
    }

    // endregion
}
