package calendar.month;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import calendar.CalendarUtils;
import calendar.common.data.ScheduleDao;
import co.muslimummah.android.R;
import co.muslimummah.android.util.UiUtils;

import static android.R.attr.x;

/**
 * Created by Jimmy on 2016/10/6 0006.
 */
public class MonthView extends View {

    private static final int NUM_COLUMNS = 7;
    private static final int NUM_ROWS = 6;
    private static final String HOLIDAY_PLACE_HOLDER = "-";
    private Paint mPaint;
    private Paint mLunarPaint;
    private int mNormalDayColor;
    private int mSelectDayColor;
    private int mSelectBGColor;
    private int mSelectBGTodayColor;
    private int mCurrentDayColor;
    private int mHintCircleColor;
    private int mLunarTextColor;
    private int mHolidayTextColor;
    private int mLastOrNextMonthTextColor;
    private int mCurrYear, mCurrMonth, mCurrDay;
    private int mSelYear, mSelMonth, mSelDay;
    private int mColumnSize, mRowSize, mSelectCircleSize;
    private int mDaySize;
    private int mLunarTextSize;
    private int mWeekRow; // 当前月份第几周
    private int mCircleRadius = 6;
    private int[][] mDaysText;
    private int[] mHolidays;
    private String[][] mHolidayOrLunarText;

    private boolean[][] mIsCurrentMonth;
    private boolean mIsShowLunar;
    private boolean mIsShowHint;
    private boolean mIsShowHolidayHint;
    private DisplayMetrics mDisplayMetrics;
    private OnMonthClickListener mDateClickListener;
    private GestureDetector mGestureDetector;
    //    private Bitmap mRestBitmap, mWorkBitmap;
    private int ummalquraYear, ummalquraMonth, ummalquraDay;
    private int toYear, toMonth, toDay;
    private int dayOffY;
    private float mHolidayDotRadius;

    private float currentDayStrokeWidth;

    public MonthView(Context context, int year, int month) {
        this(context, null, year, month);
    }

    public MonthView(Context context, TypedArray array, int year, int month) {
        this(context, array, null, year, month);
    }

    public MonthView(Context context, TypedArray array, AttributeSet attrs, int year, int month) {
        this(context, array, attrs, 0, year, month);
    }

    public MonthView(Context context, TypedArray array, AttributeSet attrs, int defStyleAttr,
        int year, int month) {
        super(context, attrs, defStyleAttr);
        initAttrs(array, year, month);
        initPaint();
        initMonth();
        initGestureDetector();

        dayOffY = (int) (4 * mDisplayMetrics.scaledDensity);
        mHolidayDotRadius = (2 * mDisplayMetrics.scaledDensity);
        currentDayStrokeWidth = (1 * mDisplayMetrics.scaledDensity);
//        initTaskHint();
    }

    private void initTaskHint() {
        if (mIsShowHint) {
            // 从数据库中获取圆点提示数据
            ScheduleDao dao = ScheduleDao.getInstance(getContext());
            CalendarUtils.getInstance(getContext())
                .addTaskHints(mSelYear, mSelMonth, dao.getTaskHintByMonth(mSelYear, mSelMonth));
        }
    }

    private void initGestureDetector() {
        mGestureDetector = new GestureDetector(getContext(),
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    doClickAction((int) e.getX(), (int) e.getY());
                    return true;
                }
            });
    }

    private void initAttrs(TypedArray array, int year, int month) {
        if (array != null) {
            mSelectDayColor = array
                .getColor(R.styleable.MonthCalendarView_month_selected_text_color,
                    Color.parseColor("#FFFFFF"));
            mSelectBGColor = array
                .getColor(R.styleable.MonthCalendarView_month_selected_circle_color,
                    Color.parseColor("#E8E8E8"));
            mSelectBGTodayColor = array
                .getColor(R.styleable.MonthCalendarView_month_selected_circle_today_color,
                    Color.parseColor("#408800"));
            mNormalDayColor = array.getColor(R.styleable.MonthCalendarView_month_normal_text_color,
                Color.parseColor("#575471"));
            mCurrentDayColor = array.getColor(R.styleable.MonthCalendarView_month_today_text_color,
                Color.parseColor("#7CC633"));
            mHintCircleColor = array.getColor(R.styleable.MonthCalendarView_month_hint_circle_color,
                Color.parseColor("#FFFFFF"));
            mLastOrNextMonthTextColor = array
                .getColor(R.styleable.MonthCalendarView_month_last_or_next_month_text_color,
                    Color.parseColor("#ACA9BC"));
            mLunarTextColor = array.getColor(R.styleable.MonthCalendarView_month_lunar_text_color,
                Color.parseColor("#408800"));
            mHolidayTextColor = array.getColor(R.styleable.MonthCalendarView_month_holiday_color,
                Color.parseColor("#A68BFF"));
            mDaySize = array.getInteger(R.styleable.MonthCalendarView_month_day_text_size, 15);
            mLunarTextSize = array
                .getInteger(R.styleable.MonthCalendarView_month_day_lunar_text_size, 10);
            mIsShowHint = array
                .getBoolean(R.styleable.MonthCalendarView_month_show_task_hint, true);
            mIsShowLunar = array.getBoolean(R.styleable.MonthCalendarView_month_show_lunar, true);
            mIsShowHolidayHint = array
                .getBoolean(R.styleable.MonthCalendarView_month_show_holiday_hint, true);
        } else {
            mSelectDayColor = Color.parseColor("#FFFFFF");
            mSelectBGColor = Color.parseColor("#E8E8E8");
            mSelectBGTodayColor = Color.parseColor("#408800");
            mNormalDayColor = Color.parseColor("#575471");
            mCurrentDayColor = Color.parseColor("#408800");
            mHintCircleColor = Color.parseColor("#FFFFFF");
            mLastOrNextMonthTextColor = Color.parseColor("#ACA9BC");
            mHolidayTextColor = Color.parseColor("#A68BFF");
            mDaySize = 15;
            mLunarTextSize = 10;
            mIsShowHint = true;
            mIsShowLunar = true;
            mIsShowHolidayHint = true;
        }

        mSelYear = year;
        mSelMonth = month;
//        mRestBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_rest_day);
//        mWorkBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_work_day);
//        mHolidays = CalendarUtils.getInstance(getContext()).getHolidays(mSelYear, mSelMonth + 1);
    }

    private void initPaint() {
        mDisplayMetrics = getResources().getDisplayMetrics();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mPaint.setTextSize(mDaySize * mDisplayMetrics.scaledDensity);

        mLunarPaint = new Paint();
        mLunarPaint.setAntiAlias(true);
        mLunarPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mLunarPaint.setTextSize(mLunarTextSize * mDisplayMetrics.scaledDensity);
        mLunarPaint.setColor(mLunarTextColor);
    }

    private void initMonth() {
        Calendar calendar = Calendar.getInstance();
        mCurrYear = calendar.get(Calendar.YEAR);
        mCurrMonth = calendar.get(Calendar.MONTH);
        mCurrDay = calendar.get(Calendar.DATE);
        if (mSelYear == mCurrYear && mSelMonth == mCurrMonth) {
            setSelectYearMonth(mSelYear, mSelMonth, mCurrDay);
        } else {
            setSelectYearMonth(mSelYear, mSelMonth, 1);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.AT_MOST) {
            heightSize = mDisplayMetrics.densityDpi * 200;
        }
        if (widthMode == MeasureSpec.AT_MOST) {
            widthSize = mDisplayMetrics.densityDpi * 300;
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        initSize();
        clearData();
        drawLastMonth(canvas);
        int selected[] = drawThisMonth(canvas);
        drawNextMonth(canvas);
        drawHintCircle(canvas);
        drawLunarText(canvas, selected);
//        drawHoliday(canvas);
    }

    private void initSize() {
        mColumnSize = getWidth() / NUM_COLUMNS;
        mRowSize = getHeight() / NUM_ROWS;
        mSelectCircleSize = (int) (mColumnSize / 2.8);
        while (mSelectCircleSize > mRowSize / 2) {
            mSelectCircleSize = (int) (mSelectCircleSize / 1.3);
        }
    }

    private void clearData() {
        mDaysText = new int[6][7];
        mHolidayOrLunarText = new String[6][7];
        mIsCurrentMonth = new boolean[6][7];
    }

    private void drawLastMonth(Canvas canvas) {
        int lastYear, lastMonth;
        if (mSelMonth == 0) {
            lastYear = mSelYear - 1;
            lastMonth = 11;
        } else {
            lastYear = mSelYear;
            lastMonth = mSelMonth - 1;
        }
        mPaint.setColor(mLastOrNextMonthTextColor);
        int monthDays = CalendarUtils.getMonthDays(lastYear, lastMonth);
        int weekNumber = CalendarUtils.getFirstDayWeek(mSelYear, mSelMonth);
        for (int day = 0; day < weekNumber - 1; day++) {
            mDaysText[0][day] = monthDays - weekNumber + day + 2;
            String dayString = String.valueOf(mDaysText[0][day]);
            int startX = (int) (mColumnSize * day
                + (mColumnSize - mPaint.measureText(dayString)) / 2);
            int startY = (int) (mRowSize / 2 - (mPaint.ascent() + mPaint.descent()) / 2);
            canvas.drawText(dayString, startX, startY - dayOffY, mPaint);
            GregorianCalendar gCal = new GregorianCalendar(lastYear, lastMonth, mDaysText[0][day]);
            Calendar uCal = new UmmalquraCalendar();
            uCal.setTime(gCal.getTime());


            ummalquraYear = uCal.get(Calendar.YEAR);
            ummalquraMonth = uCal.get(Calendar.MONTH);
            ummalquraDay = uCal.get(Calendar.DAY_OF_MONTH);
            if ((ummalquraMonth == 0 && ummalquraDay == 1) ||
                    (ummalquraMonth == 0 && ummalquraDay == 10) ||
                    (ummalquraMonth == 6 && ummalquraDay == 27) ||
                    (ummalquraMonth == 7 && ummalquraDay == 15) ||
                    (ummalquraMonth == 8 && ummalquraDay == 1) ||
                    (ummalquraMonth == 8 && ummalquraDay == 27) ||
                    (ummalquraMonth == 9 && ummalquraDay == 1) ||
                    (ummalquraMonth == 11 && ummalquraDay == 8) ||
                    (ummalquraMonth == 11 && ummalquraDay == 9) ||
                    (ummalquraMonth == 11 && ummalquraDay == 10)) {
                mHolidayOrLunarText[0][day] = HOLIDAY_PLACE_HOLDER;
            } else {
                mHolidayOrLunarText[0][day] = String.valueOf(uCal.get(Calendar.DAY_OF_MONTH));
            }

            mIsCurrentMonth[0][day] = false;
        }
    }

    private int[] drawThisMonth(Canvas canvas) {
        String dayString;
        int selectedPoint[] = new int[2];
        int monthDays = CalendarUtils.getMonthDays(mSelYear, mSelMonth);
        int weekNumber = CalendarUtils.getFirstDayWeek(mSelYear, mSelMonth);
        for (int day = 0; day < monthDays; day++) {
            dayString = String.valueOf(day + 1);
            int col = (day + weekNumber - 1) % 7;
            int row = (day + weekNumber - 1) / 7;
            mDaysText[row][col] = day + 1;
            int startX = (int) (mColumnSize * col
                + (mColumnSize - mPaint.measureText(dayString)) / 2);
            int startY = (int) (mRowSize * row + mRowSize / 2
                - (mPaint.ascent() + mPaint.descent()) / 2);

            int startRecX = mColumnSize * col;
            int startRecY = mRowSize * row;
            int endRecX = startRecX + mColumnSize;
            int endRecY = startRecY + mRowSize;

            if (dayString.equals(String.valueOf(mSelDay))) {
                //BG
                mPaint.setColor(mSelectBGTodayColor);
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle((startRecX + endRecX) / 2, (startRecY + endRecY) / 2,
                        mSelectCircleSize, mPaint);
                mWeekRow = row + 1;

                //TEXT
                selectedPoint[0] = row;
                selectedPoint[1] = col;
                mPaint.setColor(mSelectDayColor);
                mPaint.setStyle(Paint.Style.FILL);
            } else if (dayString.equals(String.valueOf(mCurrDay)) && mCurrDay != mSelDay
                && mCurrMonth == mSelMonth && mCurrYear == mSelYear) {
                //BG
                mPaint.setColor(mSelectBGTodayColor);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(currentDayStrokeWidth);
                canvas.drawCircle((startRecX + endRecX) / 2, (startRecY + endRecY) / 2,
                        mSelectCircleSize, mPaint);

                //TEXT
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(mSelectBGTodayColor);
            } else {
                mPaint.setColor(mNormalDayColor);
            }
            canvas.drawText(dayString, startX, startY - dayOffY, mPaint);

            GregorianCalendar gCal = new GregorianCalendar(mSelYear, mSelMonth,
                mDaysText[row][col]);
            Calendar uCal = new UmmalquraCalendar();
            uCal.setTime(gCal.getTime());
            ummalquraYear = uCal.get(Calendar.YEAR);
            ummalquraMonth = uCal.get(Calendar.MONTH);
            ummalquraDay = uCal.get(Calendar.DAY_OF_MONTH);
            toYear = mSelYear;
            toMonth = mSelMonth;
            toDay = mDaysText[row][col];
            if ((ummalquraMonth == 0 && ummalquraDay == 1) ||
                (ummalquraMonth == 0 && ummalquraDay == 10) ||
                (ummalquraMonth == 6 && ummalquraDay == 27) ||
                (ummalquraMonth == 7 && ummalquraDay == 15) ||
                (ummalquraMonth == 8 && ummalquraDay == 1) ||
                (ummalquraMonth == 8 && ummalquraDay == 27) ||
                (ummalquraMonth == 9 && ummalquraDay == 1) ||
                (ummalquraMonth == 11 && ummalquraDay == 8) ||
                (ummalquraMonth == 11 && ummalquraDay == 9) ||
                (ummalquraMonth == 11 && ummalquraDay == 10)) {
                mHolidayOrLunarText[row][col] = HOLIDAY_PLACE_HOLDER;
            } else {
                mHolidayOrLunarText[row][col] = String.valueOf(uCal.get(Calendar.DAY_OF_MONTH));
            }

            mIsCurrentMonth[row][col] = true;
        }
        return selectedPoint;
    }

    private void drawNextMonth(Canvas canvas) {
        mPaint.setColor(mLastOrNextMonthTextColor);
        int monthDays = CalendarUtils.getMonthDays(mSelYear, mSelMonth);
        int weekNumber = CalendarUtils.getFirstDayWeek(mSelYear, mSelMonth);
        int nextMonthDays = 42 - monthDays - weekNumber + 1;
        int nextMonth = mSelMonth + 1;
        int nextYear = mSelYear;
        if (nextMonth == 12) {
            nextMonth = 0;
            nextYear += 1;
        }
        for (int day = 0; day < nextMonthDays; day++) {
            int column = (monthDays + weekNumber - 1 + day) % 7;
            int row = 5 - (nextMonthDays - day - 1) / 7;
            try {
                mDaysText[row][column] = day + 1;
                GregorianCalendar gCal = new GregorianCalendar(nextYear, nextMonth,
                    mDaysText[row][column]);
                Calendar uCal = new UmmalquraCalendar();
                uCal.setTime(gCal.getTime());

                ummalquraYear = uCal.get(Calendar.YEAR);
                ummalquraMonth = uCal.get(Calendar.MONTH);
                ummalquraDay = uCal.get(Calendar.DAY_OF_MONTH);
                if ((ummalquraMonth == 0 && ummalquraDay == 1) ||
                        (ummalquraMonth == 0 && ummalquraDay == 10) ||
                        (ummalquraMonth == 6 && ummalquraDay == 27) ||
                        (ummalquraMonth == 7 && ummalquraDay == 15) ||
                        (ummalquraMonth == 8 && ummalquraDay == 1) ||
                        (ummalquraMonth == 8 && ummalquraDay == 27) ||
                        (ummalquraMonth == 9 && ummalquraDay == 1) ||
                        (ummalquraMonth == 11 && ummalquraDay == 8) ||
                        (ummalquraMonth == 11 && ummalquraDay == 9) ||
                        (ummalquraMonth == 11 && ummalquraDay == 10)) {
                    mHolidayOrLunarText[row][column] = HOLIDAY_PLACE_HOLDER;
                } else {
                    mHolidayOrLunarText[row][column] = String.valueOf(uCal.get(Calendar.DAY_OF_MONTH));
                }

                mIsCurrentMonth[row][column] = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
            String dayString = String.valueOf(mDaysText[row][column]);
            int startX = (int) (mColumnSize * column
                + (mColumnSize - mPaint.measureText(dayString)) / 2);
            int startY = (int) (mRowSize * row + mRowSize / 2
                - (mPaint.ascent() + mPaint.descent()) / 2);
            canvas.drawText(dayString, startX, startY - dayOffY, mPaint);
        }
    }

    /**
     * 绘制农历
     */
    private void drawLunarText(Canvas canvas, int[] selected) {
        if (mIsShowLunar) {
            for (int i = 0; i < 42; i++) {
                int column = i % 7;
                int row = i / 7;
                if (row == 0 && mDaysText[row][column] >= 23
                    || row >= 4 && mDaysText[row][column] <= 14) {
                    mLunarPaint.setColor(mLunarTextColor);
                } else {
                    mLunarPaint.setColor(mHolidayTextColor);
                }

                String dayString = mHolidayOrLunarText[row][column];
                if (!"".equals(dayString)) {
                    GregorianCalendar gCals = new GregorianCalendar(toYear, toMonth, toDay);
                    Calendar uCals = new UmmalquraCalendar();
                    uCals.setTime(gCals.getTime());
//                    if ((ummalquraMonth == 0 && dayString.equals("1")) ||
//                        (ummalquraMonth == 0 && dayString.equals("10")) ||
//                        (ummalquraMonth == 6 && dayString.equals("27")) ||
//                        (ummalquraMonth == 7 && dayString.equals("15")) ||
//                        (ummalquraMonth == 8 && dayString.equals("1")) ||
//                        (ummalquraMonth == 8 && dayString.equals("27")) ||
//                        (ummalquraMonth == 9 && dayString.equals("1")) ||
//                        (ummalquraMonth == 11 && dayString.equals("8")) ||
//                        (ummalquraMonth == 11 && dayString.equals("9")) ||
//                        (ummalquraMonth == 11 && dayString.equals("10"))) {
//                        dayString = "●";
//                    }
                    mLunarPaint.setColor(mLunarTextColor);
                }
                if (selected[0] == row && selected[1] == column) {
                    mLunarPaint.setColor(mSelectDayColor);
                }

                if (!mIsCurrentMonth[row][column]) {
                    mLunarPaint.setColor(mLastOrNextMonthTextColor);
                }

                if (HOLIDAY_PLACE_HOLDER.equals(dayString)) {
                    int startX = (mColumnSize * column + (mColumnSize) / 2);
                    int startY = (int) (mRowSize * row + mRowSize * 0.7);

                    canvas.drawCircle(startX, startY, mHolidayDotRadius, mLunarPaint);
                } else {
                    int startX = (int) (mColumnSize * column
                            + (mColumnSize - mLunarPaint.measureText(dayString)) / 2);
                    int startY = (int) (mRowSize * row + mRowSize * 0.7
                            - (mLunarPaint.ascent() + mLunarPaint.descent()) / 2);
                    canvas.drawText(dayString, startX, startY, mLunarPaint);
                }
            }
        }
    }

    /**
     * 绘制圆点提示
     */
    private void drawHintCircle(Canvas canvas) {
        if (mIsShowHint) {
            List<Integer> hints = CalendarUtils.getInstance(getContext())
                .getTaskHints(mSelYear, mSelMonth);
            if (hints.size() > 0) {
                mPaint.setColor(mHintCircleColor);
                int monthDays = CalendarUtils.getMonthDays(mSelYear, mSelMonth);
                int weekNumber = CalendarUtils.getFirstDayWeek(mSelYear, mSelMonth);
                for (int day = 0; day < monthDays; day++) {
                    int col = (day + weekNumber - 1) % 7;
                    int row = (day + weekNumber - 1) / 7;
                    if (!hints.contains(day + 1)) {
                        continue;
                    }
                    float circleX = (float) (mColumnSize * col + mColumnSize * 0.5);
                    float circleY = (float) (mRowSize * row + mRowSize * 0.75);
                    canvas.drawCircle(circleX, circleY, mCircleRadius, mPaint);
                }
            }
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    public void setSelectYearMonth(int year, int month, int day) {
        mSelYear = year;
        mSelMonth = month;
        mSelDay = day;
    }

    private void doClickAction(int x, int y) {
        if (y > getHeight()) {
            return;
        }
        int row = y / mRowSize;
        int column = x / mColumnSize;
        column = Math.min(column, 6);
        int clickYear = mSelYear, clickMonth = mSelMonth;
        if (row == 0) {
            if (mDaysText[row][column] >= 23) {
                if (mSelMonth == 0) {
                    clickYear = mSelYear - 1;
                    clickMonth = 11;
                } else {
                    clickYear = mSelYear;
                    clickMonth = mSelMonth - 1;
                }
                if (mDateClickListener != null) {
                    mDateClickListener
                        .onClickLastMonth(clickYear, clickMonth, mDaysText[row][column]);
                }
            } else {
                clickThisMonth(clickYear, clickMonth, mDaysText[row][column]);
            }
        } else {
            int monthDays = CalendarUtils.getMonthDays(mSelYear, mSelMonth);
            int weekNumber = CalendarUtils.getFirstDayWeek(mSelYear, mSelMonth);
            int nextMonthDays = 42 - monthDays - weekNumber + 1;
            if (mDaysText[row][column] <= nextMonthDays && row >= 4) {
                if (mSelMonth == 11) {
                    clickYear = mSelYear + 1;
                    clickMonth = 0;
                } else {
                    clickYear = mSelYear;
                    clickMonth = mSelMonth + 1;
                }
                if (mDateClickListener != null) {
                    mDateClickListener
                        .onClickNextMonth(clickYear, clickMonth, mDaysText[row][column]);
                }
            } else {
                clickThisMonth(clickYear, clickMonth, mDaysText[row][column]);
            }
        }
    }

    /**
     * 跳转到某日期
     */
    public void clickThisMonth(int year, int month, int day) {
        if (mDateClickListener != null) {
            mDateClickListener.onClickThisMonth(year, month, day);
        }
        setSelectYearMonth(year, month, day);
        invalidate();
    }

    /**
     * 获取当前选择年
     */
    public int getSelectYear() {
        return mSelYear;
    }

    /**
     * 获取当前选择月
     */
    public int getSelectMonth() {
        return mSelMonth;
    }

    /**
     * 获取当前选择日
     */
    public int getSelectDay() {
        return this.mSelDay;
    }

    public int getRowSize() {
        return mRowSize;
    }

    public int getWeekRow() {
        return mWeekRow;
    }

    /**
     * 添加多个圆点提示
     */
    public void addTaskHints(List<Integer> hints) {
        if (mIsShowHint) {
            CalendarUtils.getInstance(getContext()).addTaskHints(mSelYear, mSelMonth, hints);
            invalidate();
        }
    }

    /**
     * 删除多个圆点提示
     */
    public void removeTaskHints(List<Integer> hints) {
        if (mIsShowHint) {
            CalendarUtils.getInstance(getContext()).removeTaskHints(mSelYear, mSelMonth, hints);
            invalidate();
        }
    }

    /**
     * 添加一个圆点提示
     */
    public boolean addTaskHint(Integer day) {
        if (mIsShowHint) {
            if (CalendarUtils.getInstance(getContext()).addTaskHint(mSelYear, mSelMonth, day)) {
                invalidate();
                return true;
            }
        }
        return false;
    }

    /**
     * 删除一个圆点提示
     */
    public boolean removeTaskHint(Integer day) {
        if (mIsShowHint) {
            if (CalendarUtils.getInstance(getContext()).removeTaskHint(mSelYear, mSelMonth, day)) {
                invalidate();
                return true;
            }
        }
        return false;
    }

    /**
     * 设置点击日期监听
     */
    public void setOnDateClickListener(OnMonthClickListener dateClickListener) {
        this.mDateClickListener = dateClickListener;
    }

}

