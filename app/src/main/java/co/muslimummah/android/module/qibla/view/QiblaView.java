package co.muslimummah.android.module.qibla.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.R;

/**
 * Created by frank on 9/25/17.
 */
public class QiblaView extends FrameLayout {
    @BindView(R.id.iv_compass)
    ImageView ivCompass;
    @BindView(R.id.iv_mecca_pointer)
    ImageView ivMeccaPointer;
    @BindView(R.id.qtv)
    QiblaTextView qtv;

    private float mCompassOrientation = 0f;
    private float mMeccaOrientation = 0f;

    public QiblaView(Context context) {
        this(context, null);
    }

    public QiblaView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QiblaView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.layout_qibla_view, this);
        ButterKnife.bind(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        ivCompass.setPivotX(ivCompass.getMeasuredWidth() / 2);
        ivCompass.setPivotY(ivCompass.getMeasuredHeight() / 2);

        ivMeccaPointer.setPivotX(ivMeccaPointer.getMeasuredWidth() / 2);
        ivMeccaPointer.setPivotY(ivMeccaPointer.getMeasuredHeight() / 2);
    }

    /**
     * @param compassOrientation in degree.
     */
    public void setCompassOrientation(float compassOrientation) {
        this.mCompassOrientation = compassOrientation;
        ivCompass.setRotation(-mCompassOrientation);
        ivMeccaPointer.setRotation(-mCompassOrientation + mMeccaOrientation);
        qtv.setCompassOrientation(mCompassOrientation);
    }

    /**
     * @param meccaOrientation in degree.
     */
    public void setMeccaOrientation(float meccaOrientation) {
        this.mMeccaOrientation = meccaOrientation;
        ivMeccaPointer.setRotation(-mCompassOrientation + mMeccaOrientation);
    }
}
