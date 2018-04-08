package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by frank on 8/25/17.
 */
public class TouchAwareRecyclerView extends RecyclerView {
    private OnTouchListener mOnTouchListener;

    public TouchAwareRecyclerView(Context context) {
        super(context);
    }

    public TouchAwareRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchAwareRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mOnTouchListener != null) {
            mOnTouchListener.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setOnTouchListener(OnTouchListener onTouchListener) {
        this.mOnTouchListener = onTouchListener;
    }

    public interface OnTouchListener {
        void onTouchEvent(MotionEvent ev);
    }
}
