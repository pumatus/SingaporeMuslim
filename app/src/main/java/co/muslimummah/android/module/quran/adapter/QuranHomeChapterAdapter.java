package co.muslimummah.android.module.quran.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.R;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;

/**
 * Created by frank on 8/2/17.
 */

public class QuranHomeChapterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String SP_KEY_LAST_READ_VERSE = "quran.adapter.QuranHomeChapterAdapter.SP_KEY_LAST_READ_VERSE";

    private static final int VIEW_TYPE_LAST_READ_VERSE = 1;
    private static final int VIEW_TYPE_CHAPTER = 2;
    private Context mContext;
    private List<Chapter> mList;
    private Verse mLastReadVerse;
    private OnItemClickListener mOnItemClickListener;

    public QuranHomeChapterAdapter(Context context) {
        this.mContext = context;
    }

    public void update(Context context) {
        this.mContext = context;
        notifyDataSetChanged();
    }

    public void update(List<Chapter> list) {
        this.mList = list;
        notifyDataSetChanged();
    }

    public void updateLastReadVerse(Verse verse) {
        mLastReadVerse = verse;
        if (mLastReadVerse != null) {
            notifyDataSetChanged();
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onLastReadClicked(Chapter chapter, Verse verse);
        void onChapterClicked(Chapter chapter);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case VIEW_TYPE_LAST_READ_VERSE:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_quran_last_read_verse, parent, false);
                return new ViewHolderLastReadVerse(itemView);
            case VIEW_TYPE_CHAPTER:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_quran_chapter, parent, false);
                return new ViewHolderChapter(itemView);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolderChapter) {
            ((ViewHolderChapter) holder).setData(mList.get(position - getLastReadVerseCount()));
        } else if (holder instanceof ViewHolderLastReadVerse) {
            Chapter chapter = QuranRepository.INSTANCE.getChapter(mLastReadVerse.getChapterId()).blockingFirst();
            ((ViewHolderLastReadVerse) holder).setData(chapter, mLastReadVerse);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position < getLastReadVerseCount() ? VIEW_TYPE_LAST_READ_VERSE : VIEW_TYPE_CHAPTER;
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : getLastReadVerseCount() + mList.size();
    }

    private int getLastReadVerseCount() {
        return mLastReadVerse == null ? 0 : 1;
    }

    class ViewHolderLastReadVerse extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_chapter_name)
        TextView tvChapterName;
        Verse currentVerse;
        Chapter currentChapter;

        public ViewHolderLastReadVerse(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onLastReadClicked(currentChapter, currentVerse);
                    }
                }
            });
        }

        private void setData(Chapter chapter, Verse verse) {
            this.currentChapter = chapter;
            this.currentVerse = verse;
            tvChapterName.setText(String.format(Locale.US, "%s (%d)", chapter.getTransliteration(), Math.max(1, verse.getVerseId())));
        }
    }

    class ViewHolderChapter extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_chapter_id)
        TextView tvChapterId;
        @BindView(R.id.tv_title_original)
        TextView tvTitleOriginal;
        @BindView(R.id.tv_title_anglicized)
        TextView tvTitleAnglicized;
        @BindView(R.id.tv_title_translation)
        TextView tvTitleTranslation;
        @BindView(R.id.tv_verse_count)
        TextView tvVerseCount;

        private Chapter currentItem;

        public ViewHolderChapter(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            Typeface font = Typeface.createFromAsset(itemView.getContext().getAssets(), "fonts/surah-names.ttf");
            tvTitleOriginal.setTypeface(font);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onChapterClicked(currentItem);
                    }
                }
            });
        }

        private void setData(Chapter chapter) {
            currentItem = chapter;
            tvChapterId.setText(String.valueOf(currentItem.getChapterId()));
            tvTitleOriginal.setText(currentItem.getTitleInUnicode());
            tvTitleAnglicized.setText(currentItem.getTransliteration());
            tvTitleTranslation.setText(currentItem.getTranslation(mContext));
            tvVerseCount.setText(String.valueOf(currentItem.getVerseCount()));
        }
    }
}
