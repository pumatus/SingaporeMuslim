package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by tysheng
 * Date: 25/9/17 11:44 AM.
 * Email: tyshengsx@gmail.com
 */

public class VerseViewFrameLayout extends FrameLayout {

    /**
     * if true, touch event only pass to first child.
     */
    private boolean mInterceptTouch = false;

    public VerseViewFrameLayout(@NonNull Context context) {
        super(context);
    }

    public VerseViewFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VerseViewFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mInterceptTouch) {
            mChild0.dispatchTouchEvent(ev);
            return true;
        }
        return super.dispatchTouchEvent(ev);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mInterceptTouch) {
            mChild0.onTouchEvent(event);
            return false;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mInterceptTouch) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private View mChild0;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mInterceptTouch) {
            mChild0 = getChildAt(0);
        }
    }
}
