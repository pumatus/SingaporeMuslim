package co.muslimummah.android.base;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import co.muslimummah.android.util.UiUtils;

/**
 * Created by frank on 8/1/17.
 * If you want to listen to the change of keyboard's visibility, the softInputMethod of your
 * activity must be set adjustResize in AndroidManifest.xml.
 */

public class KeyboardVisibilityDelegate {
    public static KeyboardVisibilityDelegate newInstance() {
        return new KeyboardVisibilityDelegate();
    }

    private Activity mActivity;
    private KeyboardVisibilityListener mKeyboardVisibilityListener;
    private View mRootView;

    private KeyboardVisibilityDelegate() {
    }

    public void attachKeyboardVisibilityListener(@NonNull Activity activity,
                                                 @NonNull KeyboardVisibilityListener keyboardVisibilityListener) {
        if (activity == null) {
            throw new NullPointerException("Parameter: activity must not be null");
        }

        if (keyboardVisibilityListener == null) {
            throw new NullPointerException("Parameter: keyboardVisibilityListener must not be null");
        }

        int softInputMethod = activity.getWindow().getAttributes().softInputMode;
        if (WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE != softInputMethod &&
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED != softInputMethod) {
            throw new IllegalArgumentException("Parameter: activity window SoftInputMethod is not ADJUST_RESIZE");
        }

        this.mActivity = activity;
        this.mKeyboardVisibilityListener = keyboardVisibilityListener;
        this.mRootView = ((ViewGroup) mActivity.findViewById(android.R.id.content)).getChildAt(0);
        this.mRootView.getViewTreeObserver().addOnGlobalLayoutListener(mKeyboardLayoutListener);
    }

    /**
     * No need to be called if you create and store this delegate within the life cycle of an activity.
     */
    public void dettachKeyboardVisibilityListener() {
        if (this.mRootView != null) {
            this.mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(mKeyboardLayoutListener);
            this.mActivity = null;
            this.mRootView = null;
        }
    }

    private final ViewTreeObserver.OnGlobalLayoutListener mKeyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // navigation bar height
            int navigationBarHeight = UiUtils.getNavigationBarHeight(mActivity);

            // status bar height
            int statusBarHeight = UiUtils.getStatusBarHeight(mActivity);

            int keyboardHeight = mRootView.getRootView().getHeight() - (statusBarHeight + navigationBarHeight + mRootView.getHeight());

            if (keyboardHeight <= 0) {
                mKeyboardVisibilityListener.onHideKeyboard();
            } else {
                mKeyboardVisibilityListener.onShowKeyboard(keyboardHeight);
            }
        }
    };

    public interface KeyboardVisibilityListener {
        void onShowKeyboard(int keyboardHeight);

        void onHideKeyboard();
    }
}
