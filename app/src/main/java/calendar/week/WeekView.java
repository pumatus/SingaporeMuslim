package calendar.week;

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

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import calendar.CalendarUtils;
import calendar.common.data.ScheduleDao;
import co.muslimummah.android.R;

/**
 * Created by Jimmy on 2016/10/7 0007.
 */
public class WeekView extends View {

    private static final int NUM_COLUMNS = 7;
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
    private int mCurrYear, mCurrMonth, mCurrDay;
    private int mSelYear, mSelMonth, mSelDay;
    private int mColumnSize, mRowSize, mSelectCircleSize;
    private int mDaySize;
    private int mLunarTextSize;
    private int mCircleRadius = 6;
    private int[] mHolidays;
    private String mHolidayOrLunarText[];
    private boolean mIsShowLunar;
    private boolean mIsShowHint;
    private boolean mIsShowHolidayHint;
    private DateTime mStartDate;
    private DisplayMetrics mDisplayMetrics;
    private OnWeekClickListener mOnWeekClickListener;
    private GestureDetector mGestureDetector;
    private int ummalquraYear, ummalquraMonth, ummalquraDay;
//    private Bitmap mRestBitmap, mWorkBitmap;

    private int dayOffY;
    private float mHolidayDotRadius;
    private float currentDayStrokeWidth;

    public WeekView(Context context, DateTime dateTime) {
        this(context, null, dateTime);
    }

    public WeekView(Context context, TypedArray array, DateTime dateTime) {
        this(context, array, null, dateTime);
    }

    public WeekView(Context context, TypedArray array, AttributeSet attrs, DateTime dateTime) {
        this(context, array, attrs, 0, dateTime);
    }

    public WeekView(Context context, TypedArray array, AttributeSet attrs, int defStyleAttr,
        DateTime dateTime) {
        super(context, attrs, defStyleAttr);
        initAttrs(array, dateTime);
        initPaint();
        initWeek();
        initGestureDetector();

        dayOffY = (int) (4 * mDisplayMetrics.scaledDensity);
        mHolidayDotRadius = (2 * mDisplayMetrics.scaledDensity);
        currentDayStrokeWidth = (1 * mDisplayMetrics.scaledDensity);
    }

    private void initTaskHint(DateTime date) {
        if (mIsShowHint) {
            // 从数据库中获取圆点提示数据
            ScheduleDao dao = ScheduleDao.getInstance(getContext());
            if (CalendarUtils.getInstance(getContext())
                .getTaskHints(date.getYear(), date.getMonthOfYear() - 1).size() == 0) {
                CalendarUtils.getInstance(getContext())
                    .addTaskHints(date.getYear(), date.getMonthOfYear() - 1,
                        dao.getTaskHintByMonth(mSelYear, mSelMonth));
            }
        }
    }

    private void initAttrs(TypedArray array, DateTime dateTime) {
        if (array != null) {
            mSelectDayColor = array.getColor(R.styleable.WeekCalendarView_week_selected_text_color,
                Color.parseColor("#FFFFFF"));
            mSelectBGColor = array.getColor(R.styleable.WeekCalendarView_week_selected_circle_color,
                Color.parseColor("#E8E8E8"));
            mSelectBGTodayColor = array
                .getColor(R.styleable.WeekCalendarView_week_selected_circle_today_color,
                    Color.parseColor("#408800"));   //
            mNormalDayColor = array.getColor(R.styleable.WeekCalendarView_week_normal_text_color,
                Color.parseColor("#575471"));
            mCurrentDayColor = array.getColor(R.styleable.WeekCalendarView_week_today_text_color,
                Color.parseColor("#408800"));     //
            mHintCircleColor = array.getColor(R.styleable.WeekCalendarView_week_hint_circle_color,
                Color.parseColor("#FFFFFF"));
            mLunarTextColor = array.getColor(R.styleable.WeekCalendarView_week_lunar_text_color,
                Color.parseColor("#408800"));
            mHolidayTextColor = array.getColor(R.styleable.WeekCalendarView_week_holiday_color,
                Color.parseColor("#A68BFF"));
            mDaySize = array.getInteger(R.styleable.WeekCalendarView_week_day_text_size, 15);
            mLunarTextSize = array
                .getInteger(R.styleable.WeekCalendarView_week_day_lunar_text_size, 10);
            mIsShowHint = array.getBoolean(R.styleable.WeekCalendarView_week_show_task_hint, true);
            mIsShowLunar = array.getBoolean(R.styleable.WeekCalendarView_week_show_lunar, true);
            mIsShowHolidayHint = array
                .getBoolean(R.styleable.WeekCalendarView_week_show_holiday_hint, true);
        } else {
            mSelectDayColor = Color.parseColor("#FFFFFF");
            mSelectBGColor = Color.parseColor("#E8E8E8");
            mSelectBGTodayColor = Color.parseColor("#408800");
            mNormalDayColor = Color.parseColor("#575471");
            mCurrentDayColor = Color.parseColor("#408800");
            mHintCircleColor = Color.parseColor("#FFFFFF");
            mLunarTextColor = Color.parseColor("#408800");
            mHolidayTextColor = Color.parseColor("#A68BFF");
            mDaySize = 15;
            mLunarTextSize = 10;
            mIsShowHint = true;
            mIsShowLunar = true;
            mIsShowHolidayHint = true;
        }
        mStartDate = dateTime;
//        mRestBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_rest_day);
//        mWorkBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_work_day);
//        int holidays[] = CalendarUtils.getInstance(getContext()).getHolidays(mStartDate.getYear(), mStartDate.getMonthOfYear());
//        int row = CalendarUtils.getWeekRow(mStartDate.getYear(), mStartDate.getMonthOfYear() - 1, mStartDate.getDayOfMonth());
//        mHolidays = new int[7];
//        System.arraycopy(holidays, row * 7, mHolidays, 0, mHolidays.length);
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

    private void initWeek() {
        Calendar calendar = Calendar.getInstance();
        mCurrYear = calendar.get(Calendar.YEAR);
        mCurrMonth = calendar.get(Calendar.MONTH);
        mCurrDay = calendar.get(Calendar.DATE);
        DateTime endDate = mStartDate.plusDays(7);
        if (mStartDate.getMillis() <= System.currentTimeMillis() && endDate.getMillis() > System
            .currentTimeMillis()) {
            if (mStartDate.getMonthOfYear() != endDate.getMonthOfYear()) {
                if (mCurrDay < mStartDate.getDayOfMonth()) {
                    setSelectYearMonth(mStartDate.getYear(), endDate.getMonthOfYear() - 1,
                        mCurrDay);
                } else {
                    setSelectYearMonth(mStartDate.getYear(), mStartDate.getMonthOfYear() - 1,
                        mCurrDay);
                }
            } else {
                setSelectYearMonth(mStartDate.getYear(), mStartDate.getMonthOfYear() - 1, mCurrDay);
            }
        } else {
            setSelectYearMonth(mStartDate.getYear(), mStartDate.getMonthOfYear() - 1,
                mStartDate.getDayOfMonth());
        }
//        initTaskHint(mStartDate);
//        initTaskHint(endDate);
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

    public void setSelectYearMonth(int year, int month, int day) {
        mSelYear = year;
        mSelMonth = month;
        mSelDay = day;
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
        int selected = drawThisWeek(canvas);
        drawLunarText(canvas, selected);
//        drawHintCircle(canvas);
//        drawHoliday(canvas);
    }

    private void clearData() {
        mHolidayOrLunarText = new String[7];
    }

    private void initSize() {
        mColumnSize = getWidth() / NUM_COLUMNS;
        mRowSize = getHeight();
        mSelectCircleSize = (int) (mColumnSize / 2.8);
        while (mSelectCircleSize > mRowSize / 2) {
            mSelectCircleSize = (int) (mSelectCircleSize / 1.3);
        }
    }

    private int drawThisWeek(Canvas canvas) {
        int selected = 0;
        for (int i = 0; i < 7; i++) {
            DateTime date = mStartDate.plusDays(i);
            int day = date.getDayOfMonth();
            String dayString = String.valueOf(day);
            int startX = (int) (mColumnSize * i
                + (mColumnSize - mPaint.measureText(dayString)) / 2);
            int startY = (int) (mRowSize / 2 - (mPaint.ascent() + mPaint.descent()) / 2);
            int startRecX = mColumnSize * i;
            int endRecX = startRecX + mColumnSize;

            if (day == mSelDay) {
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mPaint.setColor(mSelectBGTodayColor);
                canvas.drawCircle((startRecX + endRecX) / 2, mRowSize / 2, mSelectCircleSize, mPaint);

                selected = i;
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(mSelectDayColor);
            } else if (date.getYear() == mCurrYear && date.getMonthOfYear() - 1 == mCurrMonth
                && day == mCurrDay && day != mSelDay && mCurrYear == mSelYear) {

                mPaint.setColor(mSelectBGTodayColor);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(currentDayStrokeWidth);
                canvas.drawCircle((startRecX + endRecX) / 2, mRowSize / 2, mSelectCircleSize, mPaint);

                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(mCurrentDayColor);
            } else {
                mPaint.setColor(mNormalDayColor);
            }
            canvas.drawText(dayString, startX, startY - dayOffY, mPaint);
            GregorianCalendar gCal = new GregorianCalendar(date.getYear(),
                date.getMonthOfYear() - 1, day);
            Calendar uCal = new UmmalquraCalendar();
            uCal.setTime(gCal.getTime());
            ummalquraYear = uCal.get(Calendar.YEAR);
            ummalquraMonth = uCal.get(Calendar.MONTH);
            ummalquraDay = uCal.get(Calendar.DAY_OF_MONTH);
            mHolidayOrLunarText[i] = String.valueOf(uCal.get(Calendar.DAY_OF_MONTH));
        }
        return selected;
    }

    /**
     * 绘制农历
     */
    private void drawLunarText(Canvas canvas, int selected) {
        if (mIsShowLunar) {
            for (int i = 0; i < 7; i++) {
                mLunarPaint.setColor(mHolidayTextColor);
                String dayString = mHolidayOrLunarText[i];
                if (!"".equals(dayString)) {
                    if (ummalquraMonth == 0 && dayString.equals("1") ||
                        ummalquraMonth == 0 && dayString.equals("10") ||
                        ummalquraMonth == 6 && dayString.equals("27") ||
                        ummalquraMonth == 7 && dayString.equals("15") ||
                        ummalquraMonth == 8 && dayString.equals("1") ||
                        ummalquraMonth == 8 && dayString.equals("27") ||
                        ummalquraMonth == 9 && dayString.equals("1") ||
                        ummalquraMonth == 11 && dayString.equals("8") ||
                        ummalquraMonth == 11 && dayString.equals("9") ||
                        ummalquraMonth == 11 && dayString.equals("10")) {
                        dayString = "";
                    }
                    mLunarPaint.setColor(mLunarTextColor);
                }
                if (i == selected) {
                    mLunarPaint.setColor(mSelectDayColor);
                }
                if ("".equals(dayString)) {
                    int startX = (mColumnSize * i + (mColumnSize) / 2);
                    int startY = (int) (mRowSize * 0.7);

                    canvas.drawCircle(startX, startY, mHolidayDotRadius, mLunarPaint);
                } else {
                    int startX = (int) (mColumnSize * i
                            + (mColumnSize - mLunarPaint.measureText(dayString)) / 2);
                    int startY = (int) (mRowSize * 0.72
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
            mPaint.setColor(mHintCircleColor);
            int startMonth = mStartDate.getMonthOfYear();
            int endMonth = mStartDate.plusDays(7).getMonthOfYear();
            int startDay = mStartDate.getDayOfMonth();
            if (startMonth == endMonth) {
                List<Integer> hints = CalendarUtils.getInstance(getContext())
                    .getTaskHints(mStartDate.getYear(), mStartDate.getMonthOfYear() - 1);
                for (int i = 0; i < 7; i++) {
                    drawHintCircle(hints, startDay + i, i, canvas);
                }
            } else {
                for (int i = 0; i < 7; i++) {
                    List<Integer> hints = CalendarUtils.getInstance(getContext())
                        .getTaskHints(mStartDate.getYear(), mStartDate.getMonthOfYear() - 1);
                    List<Integer> nextHints = CalendarUtils.getInstance(getContext())
                        .getTaskHints(mStartDate.getYear(), mStartDate.getMonthOfYear());
                    DateTime date = mStartDate.plusDays(i);
                    int month = date.getMonthOfYear();
                    if (month == startMonth) {
                        drawHintCircle(hints, date.getDayOfMonth(), i, canvas);
                    } else {
                        drawHintCircle(nextHints, date.getDayOfMonth(), i, canvas);
                    }
                }
            }
        }
    }

    private void drawHintCircle(List<Integer> hints, int day, int col, Canvas canvas) {
        if (!hints.contains(day)) {
            return;
        }
        float circleX = (float) (mColumnSize * col + mColumnSize * 0.5);
        float circleY = (float) (mRowSize * 0.75);
        canvas.drawCircle(circleX, circleY, mCircleRadius, mPaint);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    private void doClickAction(int x, int y) {
        if (y > getHeight() || mColumnSize == 0) {
            return;
        }
        int column = x / mColumnSize;
        column = Math.min(column, 6);
        DateTime date = mStartDate.plusDays(column);
        clickThisWeek(date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
    }

    public void clickThisWeek(int year, int month, int day) {
        if (mOnWeekClickListener != null) {
            mOnWeekClickListener.onClickDate(year, month, day);
        }
        setSelectYearMonth(year, month, day);
        invalidate();
    }

    public void setOnWeekClickListener(OnWeekClickListener onWeekClickListener) {
        mOnWeekClickListener = onWeekClickListener;
    }

    public DateTime getStartDate() {
        return mStartDate;
    }

    public DateTime getEndDate() {
        return mStartDate.plusDays(6);
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
    public void addTaskHint(Integer day) {
        if (mIsShowHint) {
            if (CalendarUtils.getInstance(getContext()).addTaskHint(mSelYear, mSelMonth, day)) {
                invalidate();
            }
        }
    }

    /**
     * 删除一个圆点提示
     */
    public void removeTaskHint(Integer day) {
        if (mIsShowHint) {
            if (CalendarUtils.getInstance(getContext()).removeTaskHint(mSelYear, mSelMonth, day)) {
                invalidate();
            }
        }
    }

}
