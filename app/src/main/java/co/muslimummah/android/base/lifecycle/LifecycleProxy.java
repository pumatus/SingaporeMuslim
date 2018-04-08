package co.muslimummah.android.base.lifecycle;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import com.trello.rxlifecycle2.LifecycleProvider;
import com.trello.rxlifecycle2.LifecycleTransformer;
import com.trello.rxlifecycle2.OutsideLifecycleException;
import com.trello.rxlifecycle2.RxLifecycle;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;

import static com.trello.rxlifecycle2.RxLifecycle.bind;

/**
 * Created by Xingbo.Jie on 3/8/17.
 */

public class LifecycleProxy implements LifecycleProvider<ScreenEvent> {

    private final BehaviorSubject<ScreenEvent> lifecycleSubject = BehaviorSubject.create();

    @Override
    @NonNull
    @CheckResult
    public final Observable<ScreenEvent> lifecycle() {
        return lifecycleSubject.hide();
    }

    @Override
    @NonNull
    @CheckResult
    public final <T> LifecycleTransformer<T> bindUntilEvent(@NonNull ScreenEvent event) {
        return RxLifecycle.bindUntilEvent(lifecycleSubject, event);
    }

    @Override
    @NonNull
    @CheckResult
    public final <T> LifecycleTransformer<T> bindToLifecycle() {
        return bind(lifecycleSubject, SCREEN_LIFECYCLE);
    }

    private static final Function<ScreenEvent, ScreenEvent> SCREEN_LIFECYCLE =
            new Function<ScreenEvent, ScreenEvent>() {
                @Override
                public ScreenEvent apply(ScreenEvent lastEvent) throws Exception {
                    switch (lastEvent) {
                        case CREATE:
                            return ScreenEvent.DESTROY;
                        case START:
                            return ScreenEvent.STOP;
                        case RESUME:
                            return ScreenEvent.PAUSE;
                        case PAUSE:
                            return ScreenEvent.STOP;
                        case STOP:
                            return ScreenEvent.DESTROY;
                        case DESTROY:
                            throw new OutsideLifecycleException("Cannot bind to Activity lifecycle when outside of it.");
                        default:
                            throw new UnsupportedOperationException("Binding to " + lastEvent + " not yet implemented");
                    }
                }
            };

    public final void onCreate() {
        lifecycleSubject.onNext(ScreenEvent.CREATE);
    }

    public final void onStart() {
        lifecycleSubject.onNext(ScreenEvent.START);
    }

    public final void onResume() {
        lifecycleSubject.onNext(ScreenEvent.RESUME);
    }

    public final void onPause() {
        lifecycleSubject.onNext(ScreenEvent.PAUSE);
    }

    public final void onStop() {
        lifecycleSubject.onNext(ScreenEvent.STOP);
    }

    public final void onDestroy() {
        lifecycleSubject.onNext(ScreenEvent.DESTROY);
    }
}
