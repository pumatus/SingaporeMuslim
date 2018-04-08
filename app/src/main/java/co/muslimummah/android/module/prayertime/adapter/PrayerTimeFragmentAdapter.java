package co.muslimummah.android.module.prayertime.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import java.util.List;

/**
 * Created by Hongd on 2017/8/12.
 */

public class PrayerTimeFragmentAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragmentList;

    public PrayerTimeFragmentAdapter(FragmentManager fm, List<Fragment> fragmentList) {
        super(fm);
        this.fragmentList = fragmentList;
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }
}
