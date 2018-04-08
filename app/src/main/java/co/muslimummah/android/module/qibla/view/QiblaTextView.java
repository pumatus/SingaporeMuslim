package co.muslimummah.android.module.qibla.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import co.muslimummah.android.R;
import co.muslimummah.android.util.UiUtils;

/**
 * Created by frank on 9/27/17.
 */
public class QiblaTextView extends View {
    private final float mDegreeTextRadius;
    private final float mOrientationTextRadius;
    private final String[] mOrientationTexts;

    private float mCompassOrientation = 0f;
    private Paint mPaint;
    private Rect tmpRect;

    public QiblaTextView(Context context) {
        this(context, null);
    }

    public QiblaTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QiblaTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mDegreeTextRadius = UiUtils.dp2px(150);
        mOrientationTextRadius = UiUtils.dp2px(85);
        mOrientationTexts = new String[]{getString(R.string.north), getString(R.string.east), getString(R.string.south), getString(R.string.west)};

        init(context, attrs);
    }
    private String getString(int id){
        return getContext().getString(id);
    }

    private void init(Context context, AttributeSet attrs) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(ContextCompat.getColor(context, R.color.white));
        mPaint.setTypeface(UiUtils.getTypefaceFromAssetPath(context, "fonts/compass-roboto-light.ttf"));
        tmpRect = new Rect();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        setPivotX(getMeasuredWidth() / 2);
        setPivotY(getMeasuredHeight() / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Draw degree text: 0, 30, 60, etc...
        mPaint.setTextSize(UiUtils.dp2px(13));
        for (int i = 0; i < 12; i++) {
            int targetDegree = i * 30;
            String targetText = String.valueOf(targetDegree);

            mPaint.getTextBounds(targetText, 0, targetText.length(), tmpRect);
            float centerX = (float) (getPivotX() + mDegreeTextRadius * Math.cos(Math.toRadians(-mCompassOrientation - 90 + targetDegree)));
            float centerY = (float) (getPivotY() + mDegreeTextRadius * Math.sin(Math.toRadians(-mCompassOrientation - 90 + targetDegree)));
            canvas.drawText(targetText, centerX - tmpRect.width() / 2, centerY + tmpRect.height() / 2, mPaint);
        }

        //Draw orientation text: N, E, S, W.
        mPaint.setTextSize(UiUtils.dp2px(21));
        for (int i = 0; i < 4; ++i) {
            int targetDegree = i * 90;
            String targetText = mOrientationTexts[i];

            mPaint.getTextBounds(targetText, 0, targetText.length(), tmpRect);
            float centerX = (float) (getPivotX() + mOrientationTextRadius * Math.cos(Math.toRadians(-mCompassOrientation - 90 + targetDegree)));
            float centerY = (float) (getPivotY() + mOrientationTextRadius * Math.sin(Math.toRadians(-mCompassOrientation - 90 + targetDegree)));
            canvas.drawText(targetText, centerX - tmpRect.width() / 2, centerY + tmpRect.height() / 2, mPaint);
        }
    }

    /**
     * @param compassOrientation in degree.
     */
    public void setCompassOrientation(float compassOrientation) {
        this.mCompassOrientation = compassOrientation;
        invalidate();
    }
}
