package co.muslimummah.android.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.trello.rxlifecycle2.LifecycleProvider;

import co.muslimummah.android.base.lifecycle.LifecycleProxy;
import co.muslimummah.android.base.lifecycle.ScreenEvent;

/**
 * Created by Xingbo.Jie on 5/8/17.
 */

public class BaseFragment extends Fragment {
    private final LifecycleProxy lifecycleProxy = new LifecycleProxy();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifecycleProxy.onCreate();
    }

    @Override
    public void onStart() {
        super.onStart();
        lifecycleProxy.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        lifecycleProxy.onResume();
    }

    @Override
    public void onPause() {
        lifecycleProxy.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        lifecycleProxy.onStop();
        super.onStop();
    }

    @Override

    public void onDestroy() {
        lifecycleProxy.onDestroy();
        super.onDestroy();
    }

    protected LifecycleProvider<ScreenEvent> lifecycleProvider() {
        return lifecycleProxy;
    }
}
