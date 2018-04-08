package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

/**
 * Created by tysheng
 * Date: 23/9/17 12:03 PM.
 * Email: tyshengsx@gmail.com
 */

public class ReverseViewPager extends ViewPager {

    private boolean reversed = true;

    public ReverseViewPager(Context context) {
        super(context);
    }

    public ReverseViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private int total;

    public void setDataSize(int dataSize) {
        this.total = dataSize - 1;
    }

    private int realItem(int item) {
        if (reversed) {
            return total - item;
        }
        return item;
    }

    @Override
    public void setCurrentItem(int item) {

        super.setCurrentItem(realItem(item));
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(realItem(item), smoothScroll);
    }

    public void setPositionInVerseItem(int item) {
        super.setCurrentItem(realItem(item), false);
    }

}
