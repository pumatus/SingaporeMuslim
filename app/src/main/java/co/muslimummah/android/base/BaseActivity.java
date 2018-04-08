package co.muslimummah.android.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.trello.rxlifecycle2.LifecycleProvider;

import co.muslimummah.android.base.lifecycle.LifecycleProxy;
import co.muslimummah.android.base.lifecycle.ScreenEvent;

/**
 * Created by frank on 8/1/17.
 */

public class BaseActivity extends AppCompatActivity {
    private final LifecycleProxy lifecycleProxy = new LifecycleProxy();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifecycleProxy.onCreate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        lifecycleProxy.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        lifecycleProxy.onResume();
    }

    @Override
    protected void onPause() {
        lifecycleProxy.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        lifecycleProxy.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        lifecycleProxy.onDestroy();
        super.onDestroy();
    }

    protected LifecycleProvider<ScreenEvent> lifecycleProvider() {
        return lifecycleProxy;
    }
}
