package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.R;
import co.muslimummah.android.base.SimpleDividerItemDecoration;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.util.UiUtils;

/**
 * Created by frank on 8/17/17.
 */

public class VerseSelectorPopup extends PopupWindow {
    @BindView(R.id.rv_verses)
    RecyclerView rvVerses;
    VerseSelectorAdapter mAdapter;

    public VerseSelectorPopup(Context context, List<Verse> verses) {
        super(context);
        setOutsideTouchable(true);
        setFocusable(true);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        View contentView = LayoutInflater.from(context).inflate(R.layout.layout_verse_selector_popup, null);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(UiUtils.dp2px(Math.min(8, verses.size()) * 48));

        setContentView(contentView);
        ButterKnife.bind(this, contentView);

        mAdapter = new VerseSelectorAdapter(verses);
        rvVerses.setLayoutManager(new LinearLayoutManager(context));
        SimpleDividerItemDecoration simpleDividerItemDecoration = new SimpleDividerItemDecoration(context);
        simpleDividerItemDecoration.setPaddingLeft(UiUtils.dp2px(16));
        rvVerses.addItemDecoration(simpleDividerItemDecoration);
        rvVerses.setAdapter(mAdapter);
    }

    private OnVerseClickListener mOnVerseClickListener;

    public void setOnVerseClickListener(OnVerseClickListener onVerseClickListener) {
        this.mOnVerseClickListener = onVerseClickListener;
    }

    public interface OnVerseClickListener {
        void onVerseClicked(Verse verse);

        void onBookmarkClicked(Verse verse);
    }

    public class VerseSelectorAdapter extends RecyclerView.Adapter<VerseSelectorAdapter.ViewHolder> {
        List<Verse> mList;

        public VerseSelectorAdapter(List<Verse> verses) {
            this.mList = verses;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_quran_dropdown_verse, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.setData(mList.get(position));
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.tv_verse_name)
            TextView tvVerseName;
            @BindView(R.id.iv_bookmark)
            ImageView ivBookmark;
            private Verse currentItem;

            ViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnVerseClickListener != null) {
                            mOnVerseClickListener.onVerseClicked(currentItem);
                        }
                    }
                });
                ivBookmark.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnVerseClickListener != null) {
                            mOnVerseClickListener.onBookmarkClicked(currentItem);
                        }
                        setData(currentItem);
                    }
                });
            }

            void setData(Verse verse) {
                currentItem = verse;
                tvVerseName.setText(String.format(Locale.US, "%s %d", tvVerseName.getContext().getString(R.string.verse), verse.getVerseId()));
                ivBookmark.setImageResource(verse.getIsBookMarked() ? R.drawable.ic_btn_bookmark_on : R.drawable.ic_btn_bookmark_off);
            }
        }
    }
}
