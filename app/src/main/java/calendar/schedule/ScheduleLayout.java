package calendar.schedule;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.List;

import calendar.CalendarUtils;
import calendar.OnCalendarClickListener;
import calendar.month.MonthCalendarView;
import calendar.month.MonthView;
import calendar.week.WeekCalendarView;
import calendar.week.WeekView;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.GA;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import timber.log.Timber;

/**
 * Created by Jimmy on 2016/10/7 0007.
 */
public class ScheduleLayout extends FrameLayout {

    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private final int DEFAULT_MONTH = 0;
    private final int DEFAULT_WEEK = 1;

    private MonthCalendarView mcvCalendar;
    private WeekCalendarView wcvCalendar;
    private RelativeLayout rlMonthCalendar;
    private RelativeLayout rlScheduleList;
    private ScheduleRecyclerView rvScheduleList;

    private int mCurrentSelectYear;
    private int mCurrentSelectMonth;
    private int mCurrentSelectDay;
    private int mRowSize;
    private int mMinDistance;
    private int mAutoScrollDistance;
    private int mDefaultView;
    private float mDownPosition[] = new float[2];
    private boolean mIsScrolling = false;
    private boolean mIsAutoChangeMonthRow;
    private boolean mCurrentRowsIsSix = true;

    private ScheduleState mState;
    private OnCalendarClickListener mOnCalendarClickListener;
    private GestureDetector mGestureDetector;
    private GestureDetectorCompat mSwipeDetector;

    public ScheduleLayout(Context context) {
        this(context, null);
    }

    public ScheduleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScheduleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context.obtainStyledAttributes(attrs, R.styleable.ScheduleLayout));
        initDate();
        initGestureDetector();
    }

    private void initAttrs(TypedArray array) {
        mDefaultView = array.getInt(R.styleable.ScheduleLayout_default_view, DEFAULT_MONTH);
        mIsAutoChangeMonthRow = array
                .getBoolean(R.styleable.ScheduleLayout_auto_change_month_row, true);
        array.recycle();
        mState = ScheduleState.CLOSE;
        mRowSize = getResources().getDimensionPixelSize(R.dimen.week_calendar_height);
        mMinDistance = getResources().getDimensionPixelSize(R.dimen.calendar_min_distance);
        mAutoScrollDistance = getResources().getDimensionPixelSize(R.dimen.auto_scroll_distance);
    }

    private void initGestureDetector() {
        mGestureDetector = new GestureDetector(getContext(), new OnScheduleScrollListener(this));
        mSwipeDetector = new GestureDetectorCompat(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityY) < Math.abs(velocityX)) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
                    if (velocityX > SWIPE_THRESHOLD_VELOCITY) {
                        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - 1);

                        ThirdPartyAnalytics.INSTANCE.logEvent(GA.Category.Calendar,
                                GA.Action.Swipe,
                                GA.Label.PreviousDay);

                    } else if (-velocityX > SWIPE_THRESHOLD_VELOCITY) {
                        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1);

                        ThirdPartyAnalytics.INSTANCE.logEvent(GA.Category.Calendar,
                                GA.Action.Swipe,
                                GA.Label.NextDay);
                    } else {
                        calendar = null;
                    }

                    if (calendar != null) {
                        int year = calendar.get(Calendar.YEAR);
                        int month = calendar.get(Calendar.MONTH);
                        int day = calendar.get(Calendar.DAY_OF_MONTH);

                        int weeks = CalendarUtils.getWeeksAgo(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay, year, month, day);
                        int months = CalendarUtils.getMonthsAgo(mCurrentSelectYear, mCurrentSelectMonth, year, month);

                        updateWeekView(year, month, day, weeks);
                        updateMonthView(year, month, day, months);
                    }
                }
                Timber.d("onFling %f, %f", velocityX, velocityY);
                return true;
            }
        });
    }

    private void initDate() {
        Calendar calendar = Calendar.getInstance();
        resetCurrentSelectDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mcvCalendar = (MonthCalendarView) findViewById(R.id.mcvCalendar);
        wcvCalendar = (WeekCalendarView) findViewById(R.id.wcvCalendar);
        rlMonthCalendar = (RelativeLayout) findViewById(R.id.rlMonthCalendar);
        rlScheduleList = (RelativeLayout) findViewById(R.id.rlScheduleList);
        rvScheduleList = (ScheduleRecyclerView) findViewById(R.id.rvScheduleList);
        bindingMonthAndWeekCalendar();
    }

    private void bindingMonthAndWeekCalendar() {
        mcvCalendar.setOnCalendarClickListener(mMonthCalendarClickListener);
        wcvCalendar.setOnCalendarClickListener(mWeekCalendarClickListener);
        // 初始化视图
        Calendar calendar = Calendar.getInstance();
        if (mIsAutoChangeMonthRow) {
            mCurrentRowsIsSix = CalendarUtils
                    .getMonthRows(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)) == 6;
        }
        if (mDefaultView == DEFAULT_MONTH) {
            wcvCalendar.setVisibility(INVISIBLE);
            mState = ScheduleState.OPEN;
            if (!mCurrentRowsIsSix) {
                rlScheduleList.setY(rlScheduleList.getY() - mRowSize);
            }
        } else if (mDefaultView == DEFAULT_WEEK) {
            wcvCalendar.setVisibility(VISIBLE);
            mState = ScheduleState.CLOSE;
            int row = CalendarUtils
                    .getWeekRow(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH));
            rlMonthCalendar.setY(-row * mRowSize);
            rlScheduleList.setY(rlScheduleList.getY() - 5 * mRowSize);
        }
    }

    private void resetCurrentSelectDate(int year, int month, int day) {
        mCurrentSelectYear = year;
        mCurrentSelectMonth = month;
        mCurrentSelectDay = day;
    }

    private OnCalendarClickListener mMonthCalendarClickListener = new OnCalendarClickListener() {
        @Override
        public void onClickDate(int year, int month, int day) {
            int weeks = CalendarUtils
                    .getWeeksAgo(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay, year,
                            month, day);
            updateWeekView(year, month, day, weeks);
        }

        @Override
        public void onPageChange(int year, int month, int day) {
            computeCurrentRowsIsSix(year, month);
        }
    };

    private void updateWeekView(int year, int month, int day, int weeks) {
        wcvCalendar.setOnCalendarClickListener(null);

        resetCurrentSelectDate(year, month, day);
        int position = wcvCalendar.getCurrentItem() + weeks;
        if (weeks != 0) {
            wcvCalendar.setCurrentItem(position, false);
        }
        resetWeekView(position);
        wcvCalendar.setOnCalendarClickListener(mWeekCalendarClickListener);
    }

    private void computeCurrentRowsIsSix(int year, int month) {
        if (mIsAutoChangeMonthRow) {
            boolean isSixRow = CalendarUtils.getMonthRows(year, month) == 6;
            if (mCurrentRowsIsSix != isSixRow) {
                mCurrentRowsIsSix = isSixRow;
                if (mState == ScheduleState.OPEN) {
                    if (mCurrentRowsIsSix) {
                        AutoMoveAnimation animation = new AutoMoveAnimation(rlScheduleList,
                                mRowSize);
                        rlScheduleList.startAnimation(animation);
                    } else {
                        AutoMoveAnimation animation = new AutoMoveAnimation(rlScheduleList,
                                -mRowSize);
                        rlScheduleList.startAnimation(animation);
                    }
                }
            }
        }
    }

    private void resetWeekView(int position) {
        WeekView weekView = wcvCalendar.getCurrentWeekView();
        if (weekView != null) {
            weekView.setSelectYearMonth(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
            weekView.invalidate();
        } else {
            WeekView newWeekView = wcvCalendar.getWeekAdapter().instanceWeekView(position);
            newWeekView
                    .setSelectYearMonth(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
            newWeekView.invalidate();
            wcvCalendar.setCurrentItem(position);
        }
        if (mOnCalendarClickListener != null) {
            mOnCalendarClickListener
                    .onClickDate(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
        }
    }

    private OnCalendarClickListener mWeekCalendarClickListener = new OnCalendarClickListener() {
        @Override
        public void onClickDate(int year, int month, int day) {
            updateMonthView(year,
                    month,
                    day,
                    CalendarUtils.getMonthsAgo(mCurrentSelectYear, mCurrentSelectMonth, year, month));
        }

        @Override
        public void onPageChange(int year, int month, int day) {
            if (mIsAutoChangeMonthRow) {
                if (mCurrentSelectMonth != month) {
                    mCurrentRowsIsSix = CalendarUtils.getMonthRows(year, month) == 6;
                }
            }
        }
    };

    private void updateMonthView(int year, int month, int day, int months) {
        mcvCalendar.setOnCalendarClickListener(null);
        resetCurrentSelectDate(year, month, day);
        if (months != 0) {
            int position = mcvCalendar.getCurrentItem() + months;
            mcvCalendar.setCurrentItem(position, false);
        }
        resetMonthView();
        mcvCalendar.setOnCalendarClickListener(mMonthCalendarClickListener);
        if (mIsAutoChangeMonthRow) {
            mCurrentRowsIsSix = CalendarUtils.getMonthRows(year, month) == 6;
        }
    }

    private void resetMonthView() {
        MonthView monthView = mcvCalendar.getCurrentMonthView();
        if (monthView != null) {
            monthView
                    .setSelectYearMonth(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
            monthView.invalidate();
        }
        if (mOnCalendarClickListener != null) {
            mOnCalendarClickListener
                    .onClickDate(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
        }
        resetCalendarPosition();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//        resetViewHeight(this, height);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        resetViewHeight(rlScheduleList, getMeasuredHeight() - mRowSize);
    }

    private void resetViewHeight(View view, int height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams.height != height) {
            layoutParams.height = height;
            view.setLayoutParams(layoutParams);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getY() > rlScheduleList.getY()) {
            mSwipeDetector.onTouchEvent(ev);
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownPosition[0] = ev.getRawX();
                mDownPosition[1] = ev.getRawY();
                mGestureDetector.onTouchEvent(ev);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mIsScrolling) {
            return true;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                float x = ev.getRawX();
                float y = ev.getRawY();
                float distanceX = Math.abs(x - mDownPosition[0]);
                float distanceY = Math.abs(y - mDownPosition[1]);
                if (distanceY > mMinDistance && distanceY > distanceX * 2.0f) {
                    return (y > mDownPosition[1] && isRecyclerViewTouch()) || (y < mDownPosition[1]
                            && mState == ScheduleState.OPEN);
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean isRecyclerViewTouch() {
        return mState == ScheduleState.CLOSE && (rvScheduleList.getChildCount() == 0
                || rvScheduleList.isScrollTop());
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownPosition[0] = event.getRawX();
                mDownPosition[1] = event.getRawY();
                resetCalendarPosition();
                return true;
            case MotionEvent.ACTION_MOVE:
                transferEvent(event);
                mIsScrolling = true;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                transferEvent(event);
                changeCalendarState();
                resetScrollingState();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void transferEvent(MotionEvent event) {
        if (mState == ScheduleState.CLOSE) {
            mcvCalendar.setVisibility(VISIBLE);
            wcvCalendar.setVisibility(INVISIBLE);
            mGestureDetector.onTouchEvent(event);
        } else {
            mGestureDetector.onTouchEvent(event);
        }
    }

    private void changeCalendarState() {
        if (rlScheduleList.getY() > mRowSize * 2 &&
                rlScheduleList.getY() < mcvCalendar.getHeight() - mRowSize) { // 位于中间
            ScheduleAnimation animation = new ScheduleAnimation(this, mState, mAutoScrollDistance);
            animation.setDuration(300);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    changeState();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            rlScheduleList.startAnimation(animation);
        } else if (rlScheduleList.getY() <= mRowSize * 2) { // 位于顶部
            ScheduleAnimation animation = new ScheduleAnimation(this, ScheduleState.OPEN,
                    mAutoScrollDistance);
            animation.setDuration(50);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mState == ScheduleState.OPEN) {
                        changeState();
                    } else {
                        resetCalendar();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            rlScheduleList.startAnimation(animation);
        } else {
            ScheduleAnimation animation = new ScheduleAnimation(this, ScheduleState.CLOSE,
                    mAutoScrollDistance);
            animation.setDuration(50);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mState == ScheduleState.CLOSE) {
                        mState = ScheduleState.OPEN;
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            rlScheduleList.startAnimation(animation);
        }
    }

    private void resetCalendarPosition() {
        if (mState == ScheduleState.OPEN) {
            rlMonthCalendar.setY(0);
            if (mCurrentRowsIsSix) {
                rlScheduleList.setY(mcvCalendar.getHeight());
            } else {
                rlScheduleList.setY(mcvCalendar.getHeight() - mRowSize);
            }
        } else {
            rlMonthCalendar.setY(-CalendarUtils
                    .getWeekRow(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay) * mRowSize);
            rlScheduleList.setY(mRowSize);
        }
    }

    /**
     * Description: set expanded or not
     * Creator:  tysheng
     * Update Date:  20/9/17 6:00 PM
     */
    public void setCalendarExpanded(boolean expanded) {
        Timber.d("expanded %b,mState %s", expanded, mState.toString());
        if (expanded) {
            if (ScheduleState.CLOSE == mState) {
                mState = ScheduleState.OPEN;
                wcvCalendar.setVisibility(INVISIBLE);

                rlMonthCalendar.setY(0);
                rlScheduleList.setY( mcvCalendar.getHeight());
//                invalidate();
            }
        } else {
            if (ScheduleState.OPEN == mState) {
                mState = ScheduleState.CLOSE;
                wcvCalendar.setVisibility(VISIBLE);

                rlMonthCalendar.setY(0);
                rlScheduleList.setY( mcvCalendar.getHeight());
                int row = CalendarUtils
                        .getWeekRow(getCurrentSelectYear(),getCurrentSelectMonth(),
                                getCurrentSelectDay());
                rlMonthCalendar.setY(-row * mRowSize);
                rlScheduleList.setY(rlScheduleList.getY() - 5 * mRowSize);
            }
        }
    }

    private void resetCalendar() {
        if (mState == ScheduleState.OPEN) {
            mcvCalendar.setVisibility(VISIBLE);
            wcvCalendar.setVisibility(INVISIBLE);
        } else {
            mcvCalendar.setVisibility(INVISIBLE);
            wcvCalendar.setVisibility(VISIBLE);
        }
    }

    private void changeState() {
        if (mState == ScheduleState.OPEN) {
            mState = ScheduleState.CLOSE;
            mcvCalendar.setVisibility(INVISIBLE);
            wcvCalendar.setVisibility(VISIBLE);
            rlMonthCalendar.setY((1 - mcvCalendar.getCurrentMonthView().getWeekRow()) * mRowSize);
            checkWeekCalendar();
//            ThirdPartyAnalytics.INSTANCE.logEvent("Calendar", "Swipe", "WeekToMonth", null);

            ThirdPartyAnalytics.INSTANCE.logEvent(GA.Category.Calendar,
                    GA.Action.Swipe,
                    GA.Label.WeekToMonth);
        } else {
            mState = ScheduleState.OPEN;
            mcvCalendar.setVisibility(VISIBLE);
            wcvCalendar.setVisibility(INVISIBLE);
            rlMonthCalendar.setY(0);
            ThirdPartyAnalytics.INSTANCE.logEvent(GA.Category.Calendar,
                    GA.Action.Swipe,
                    GA.Label.MonthToWeek);
//            ThirdPartyAnalytics.INSTANCE.logEvent("Calendar", "Swipe", "MonthToMonth", null);
        }
    }

    private void checkWeekCalendar() {
        WeekView weekView = wcvCalendar.getCurrentWeekView();
        DateTime start = weekView.getStartDate();
        DateTime end = weekView.getEndDate();
        DateTime current = new DateTime(mCurrentSelectYear, mCurrentSelectMonth + 1,
                mCurrentSelectDay, 23, 59, 59);
        int week = 0;
        while (current.getMillis() < start.getMillis()) {
            week--;
            start = start.plusDays(-7);
        }
        current = new DateTime(mCurrentSelectYear, mCurrentSelectMonth + 1, mCurrentSelectDay, 0, 0,
                0);
        if (week == 0) {
            while (current.getMillis() > end.getMillis()) {
                week++;
                end = end.plusDays(7);
            }
        }
        if (week != 0) {
            int position = wcvCalendar.getCurrentItem() + week;
            if (wcvCalendar.getWeekViews().get(position) != null) {
                wcvCalendar.getWeekViews().get(position)
                        .setSelectYearMonth(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
                wcvCalendar.getWeekViews().get(position).invalidate();
            } else {
                WeekView newWeekView = wcvCalendar.getWeekAdapter().instanceWeekView(position);
                newWeekView
                        .setSelectYearMonth(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
                newWeekView.invalidate();
            }
            wcvCalendar.setCurrentItem(position, false);
        }
    }

    private void resetScrollingState() {
        mDownPosition[0] = 0;
        mDownPosition[1] = 0;
        mIsScrolling = false;
    }

    protected void onCalendarScroll(float distanceY) {
        MonthView monthView = mcvCalendar.getCurrentMonthView();
        distanceY = Math.min(distanceY, mAutoScrollDistance);
        float calendarDistanceY = distanceY / (mCurrentRowsIsSix ? 5.0f : 4.0f);
        int row = monthView.getWeekRow() - 1;
        int calendarTop = -row * mRowSize;
        int scheduleTop = mRowSize;
        float calendarY = rlMonthCalendar.getY() - calendarDistanceY * row;
        calendarY = Math.min(calendarY, 0);
        calendarY = Math.max(calendarY, calendarTop);
        rlMonthCalendar.setY(calendarY);
        float scheduleY = rlScheduleList.getY() - distanceY;
        if (mCurrentRowsIsSix) {
            scheduleY = Math.min(scheduleY, mcvCalendar.getHeight());
        } else {
            scheduleY = Math.min(scheduleY, mcvCalendar.getHeight() - mRowSize);
        }
        scheduleY = Math.max(scheduleY, scheduleTop);
        rlScheduleList.setY(scheduleY);
    }

    public void setOnCalendarClickListener(OnCalendarClickListener onCalendarClickListener) {
        mOnCalendarClickListener = onCalendarClickListener;
    }

    private void resetMonthViewDate(final int year, final int month, final int day,
                                    final int position) {
        if (mcvCalendar.getMonthViews().get(position) == null) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    resetMonthViewDate(year, month, day, position);
                }
            }, 50);
        } else {
            mcvCalendar.getMonthViews().get(position).clickThisMonth(year, month, day);
        }
    }

    /**
     * 初始化年月日
     *
     * @param month (0-11)
     * @param day   (1-31)
     */
    public void initData(int year, int month, int day) {
        int monthDis = CalendarUtils
                .getMonthsAgo(mCurrentSelectYear, mCurrentSelectMonth, year, month);
        int position = mcvCalendar.getCurrentItem() + monthDis;
        mcvCalendar.setCurrentItem(position);
        resetMonthViewDate(year, month, day, position);
    }

    /**
     * 添加多个圆点提示
     */
    public void addTaskHints(List<Integer> hints) {
        CalendarUtils.getInstance(getContext())
                .addTaskHints(mCurrentSelectYear, mCurrentSelectMonth, hints);
        if (mcvCalendar.getCurrentMonthView() != null) {
            mcvCalendar.getCurrentMonthView().invalidate();
        }
        if (wcvCalendar.getCurrentWeekView() != null) {
            wcvCalendar.getCurrentWeekView().invalidate();
        }
    }

    /**
     * 删除多个圆点提示
     */
    public void removeTaskHints(List<Integer> hints) {
        CalendarUtils.getInstance(getContext())
                .removeTaskHints(mCurrentSelectYear, mCurrentSelectMonth, hints);
        if (mcvCalendar.getCurrentMonthView() != null) {
            mcvCalendar.getCurrentMonthView().invalidate();
        }
        if (wcvCalendar.getCurrentWeekView() != null) {
            wcvCalendar.getCurrentWeekView().invalidate();
        }
    }

    /**
     * 添加一个圆点提示
     */
    public void addTaskHint(Integer day) {
        if (mcvCalendar.getCurrentMonthView() != null) {
            if (mcvCalendar.getCurrentMonthView().addTaskHint(day)) {
                if (wcvCalendar.getCurrentWeekView() != null) {
                    wcvCalendar.getCurrentWeekView().invalidate();
                }
            }
        }
    }

    /**
     * 删除一个圆点提示
     */
    public void removeTaskHint(Integer day) {
        if (mcvCalendar.getCurrentMonthView() != null) {
            if (mcvCalendar.getCurrentMonthView().removeTaskHint(day)) {
                if (wcvCalendar.getCurrentWeekView() != null) {
                    wcvCalendar.getCurrentWeekView().invalidate();
                }
            }
        }
    }

    public ScheduleRecyclerView getSchedulerRecyclerView() {
        return rvScheduleList;
    }

    public MonthCalendarView getMonthCalendar() {
        return mcvCalendar;
    }

    public WeekCalendarView getWeekCalendar() {
        return wcvCalendar;
    }

    public int getCurrentSelectYear() {
        return mCurrentSelectYear;
    }

    public int getCurrentSelectMonth() {
        return mCurrentSelectMonth;
    }

    public int getCurrentSelectDay() {
        return mCurrentSelectDay;
    }

}
