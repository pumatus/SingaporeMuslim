package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * dictate the touch movement
 *
 * Created by tysheng
 * Date: 28/9/17 7:37 PM.
 * Email: tyshengsx@gmail.com
 */

public class TouchableToolbar extends Toolbar {
    public TouchableToolbar(Context context) {
        super(context);
    }

    public TouchableToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchableToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public interface TouchListener{
        void onTouch(MotionEvent event);
    }

    private TouchListener mTouchListener;

    public void setTouchListener(TouchListener touchListener) {
        mTouchListener = touchListener;
    }



    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mTouchListener != null) {
            mTouchListener.onTouch(ev);
        }
        return super.dispatchTouchEvent(ev);
    }
}
