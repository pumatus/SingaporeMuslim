package co.muslimummah.android.share;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;

import co.muslimummah.android.R;
import co.muslimummah.android.base.NetObserver;
import co.muslimummah.android.util.ToastUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.ResourceObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


/**
 * Created by tysheng
 * Date: 3/10/17 10:09 AM.
 * Email: tyshengsx@gmail.com
 */

public class ShareDialogFragment extends BottomSheetDialogFragment {
    public static final String TAG = "ShareDialogFragment";
    public static final String MESSAGE = "MESSAGE";
    private BottomSheetBehavior mBehavior;


    public static ShareDialogFragment newInstance(ShareMessage message) {
        Bundle args = new Bundle();
        args.putSerializable(MESSAGE, message);
        ShareDialogFragment fragment = new ShareDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        //默认全屏展开
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void dismissDialog() {
        //点击任意布局关闭
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public static final long CLICK_DEBOUNCE = 1000L;
    private long lastTimeClick;
    private ShareMessage message;
    private View progressView;
    private ResourceObserver<Intent> mObserver;
    private ShareViewPager shareViewPager;
    private boolean clickEnable = true;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        Context context = getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_share_dialog, null);
        shareViewPager = view.findViewById(R.id.shareViewPager);
        progressView = view.findViewById(R.id.progressView);
        shareViewPager.setData(ShareUtils.getShareAppList(context));
        message = (ShareMessage) getArguments().getSerializable(MESSAGE);
        shareViewPager.setOnAppClickListener(new ShareViewPager.onAppClickListener() {
            @Override
            public void onClick(int position, ShareAppInfo shareAppInfo) {
                Timber.tag(TAG).d("onClick: " + position + " msg: " + shareAppInfo.toString());
                long current = System.currentTimeMillis();
                if (current - lastTimeClick < CLICK_DEBOUNCE || !clickEnable) {
                    return;
                }
                lastTimeClick = current;

                if (message == null) {
                    return;
                }
                mObserver = provideObserver();
                Observable.just(shareAppInfo)
                        .map(new Function<ShareAppInfo, Intent>() {
                            @Override
                            public Intent apply(@io.reactivex.annotations.NonNull ShareAppInfo shareAppInfo) throws Exception {
                                return ShareUtils.handleIntentByPlatform2(shareAppInfo, message);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(mObserver);
            }
        });

        dialog.setContentView(view);
        mBehavior = BottomSheetBehavior.from((View) view.getParent());

        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mObserver != null && !mObserver.isDisposed()) {
            mObserver.dispose();
        }
    }

    private void setLoading(boolean loading) {
        if (loading) {
            progressView.setVisibility(View.VISIBLE);
            clickEnable = false;
        } else {
            progressView.setVisibility(View.GONE);
            clickEnable = true;
        }
    }

    ResourceObserver<Intent> provideObserver() {
        return new NetObserver<Intent>() {

            @Override
            protected void onStart() {
                super.onStart();
                setLoading(true);
//                                fragment.show(getChildFragmentManager(), ProgressDialogFragment.class.getSimpleName());
            }

            @Override
            public void onNext(@io.reactivex.annotations.NonNull Intent intent) {
                super.onNext(intent);

                if (ShareUtils.checkIntentHandle(getContext(), intent)) {
                    startActivity(intent);
                } else {
                    ToastUtil.show(getString(R.string.no_apps_handle));
                }
                setLoading(false);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                dismiss();
            }

            @Override
            public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                super.onError(e);
                setLoading(false);
                ToastUtil.show(getString(R.string.share_failed));
                Timber.tag(TAG).d("onError: " + e.getMessage());
            }
        };
    }
}
