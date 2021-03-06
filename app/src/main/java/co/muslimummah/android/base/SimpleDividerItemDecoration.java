package co.muslimummah.android.base;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import co.muslimummah.android.R;

/**
 * Created by frank on 8/3/17.
 */

public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDivider;
    private int mPaddingLeft;
    private int mPaddingRight;

    public SimpleDividerItemDecoration(Context context) {
        mDivider = ContextCompat.getDrawable(context, R.drawable.line_divider);
    }

    public SimpleDividerItemDecoration(Drawable dividerDrawable) {
        mDivider = dividerDrawable;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.mPaddingLeft = paddingLeft;
    }

    public void setPaddingRight(int paddingRight) {
        this.mPaddingRight = paddingRight;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft() + mPaddingLeft;
        int right = parent.getWidth() - parent.getPaddingRight() - mPaddingRight;

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mDivider.getIntrinsicHeight();

            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }
}
