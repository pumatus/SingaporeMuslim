package co.muslimummah.android.module.quran.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.BaseFragment;
import co.muslimummah.android.base.SimpleDividerItemDecoration;
import co.muslimummah.android.base.lifecycle.ScreenEvent;
import co.muslimummah.android.player.MusicServiceLogDelegate;
import co.muslimummah.android.module.prayertime.ui.activity.MainActivity;
import co.muslimummah.android.module.quran.activity.VerseActivity;
import co.muslimummah.android.module.quran.adapter.QuranHomeChapterAdapter;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import co.muslimummah.android.storage.AppSession;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by Hongd on 2017/8/14.
 */

public class QuranFragment extends BaseFragment {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.rv_chapters)
    RecyclerView rvChapters;
    public QuranHomeChapterAdapter mQuranHomeChapterAdapter;
    Unbinder unbinder;
    MusicServiceLogDelegate mMusicServiceLogDelegate;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quran, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        unbinder = ButterKnife.bind(this, view);

        setupToolbar();

        rvChapters.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChapters.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
        mQuranHomeChapterAdapter = new QuranHomeChapterAdapter(getContext());
        mQuranHomeChapterAdapter.setOnItemClickListener(new QuranHomeChapterAdapter.OnItemClickListener() {
            @Override
            public void onLastReadClicked(Chapter chapter, Verse verse) {
                mMusicServiceLogDelegate.logEvent("QuranChapterView", "LastRead", null);
                VerseActivity.start(getActivity(), chapter, verse.getVerseId());
            }

            @Override
            public void onChapterClicked(Chapter chapter) {
                mMusicServiceLogDelegate.logChapterItemClickEvent(chapter.getChapterId());
                VerseActivity.start(getActivity(), chapter);
            }
        });
        rvChapters.setAdapter(mQuranHomeChapterAdapter);

        QuranRepository.INSTANCE.getChapters()
                .compose(lifecycleProvider().<List<Chapter>>bindUntilEvent(ScreenEvent.DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<List<Chapter>>() {
                    @Override
                    public void accept(@NonNull List<Chapter> chapters) throws Exception {
                        mQuranHomeChapterAdapter.update(chapters);
                        mQuranHomeChapterAdapter.updateLastReadVerse(AppSession.getInstance(getContext()).getCachedValue(Verse.SP_KEY_LAST_READ, Verse.class));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Timber.e(throwable, "get chapters failed which is impossible");
                    }
                });

        mMusicServiceLogDelegate = new MusicServiceLogDelegate();
        mMusicServiceLogDelegate.bind(view.getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        mQuranHomeChapterAdapter.updateLastReadVerse(AppSession.getInstance(OracleApp.getInstance()).getCachedValue(Verse.SP_KEY_LAST_READ, Verse.class));
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    private void setupToolbar() {
        toolbar.inflateMenu(R.menu.menu_setting);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_setting:
                        if (getActivity() instanceof MainActivity) {
                            mMusicServiceLogDelegate.logEvent("QuranChapterView", "Setting", null);
                            ((MainActivity) getActivity()).switchDrawerLayout(true);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    public void updateTranslationContext(Context context) {
        if (isAdded()) {
            mQuranHomeChapterAdapter.update(context);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        mMusicServiceLogDelegate.unBind();
    }
}
