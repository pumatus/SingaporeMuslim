package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by frank on 8/16/17.
 */

public class HorizontalProgressBar extends View {
    private Paint mPaint;
    private Rect mRect;
    @FloatRange(from = 0f, to = 1f)
    private float mProgress;

    public HorizontalProgressBar(Context context) {
        this(context, null);
    }

    public HorizontalProgressBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mPaint = new Paint();
        mRect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mRect.set(0, 0, (int) (mProgress * getWidth()), getHeight());
        canvas.drawRect(mRect, mPaint);
    }

    public void setProgressDrawableColor(@ColorInt int color) {
        mPaint.setColor(color);
        invalidate();
    }

    public void setProgress(@FloatRange(from = 0f, to = 1f) float progress) {
        mProgress = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }
}
