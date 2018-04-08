package co.muslimummah.android.share;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import co.muslimummah.android.R;
import co.muslimummah.android.util.UiUtils;


/**
 * Created by tysheng
 * Date: 2/10/17 6:43 PM.
 * Email: tyshengsx@gmail.com
 */

public class ShareViewPager extends FrameLayout {

    private RecyclerView mViewPager;
    private LinearLayout mIndicatorLayout;
    private TextView mShareTitle;

    private int mPrePosition;
    private int mSelectedColor, mUnSelectedColor;
    private List<ShareAppInfo> mAppInfoList;
    private onAppClickListener mOnAppClickListener;
    private static final int PAGE_APPS_COUNT = 8;


    public interface onAppClickListener {
        void onClick(int position, ShareAppInfo shareAppInfo);
    }

    public void setOnAppClickListener(onAppClickListener onAppClickListener) {
        mOnAppClickListener = onAppClickListener;
    }

    public ShareViewPager(Context context) {
        this(context, null, 0);
    }

    public ShareViewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShareViewPager(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mAppInfoList = new ArrayList<>();
        View view = LayoutInflater.from(context).inflate(R.layout.view_share, null);
        mViewPager = (RecyclerView) view.findViewById(R.id.rv_content);
        mIndicatorLayout = (LinearLayout) view.findViewById(R.id.ll_indicator);
        mShareTitle = (TextView) view.findViewById(R.id.tv_title);
        addView(view);
        mSelectedColor = 0xff7d7d7d;
        mUnSelectedColor = 0xffdcdcdc;
    }

    public void setShareTitle(String title) {
        mShareTitle.setText(title);
    }

    private LinearLayoutManager mLayoutManager;

    public void setData(List<ShareAppInfo> list) {
        mAppInfoList = list;
        mViewPager.removeAllViews();
        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        mViewPager.setLayoutManager(mLayoutManager);
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(mViewPager);
        RecyclerView.Adapter adapter = new Adapter();
        mViewPager.setAdapter(adapter);
        mIndicatorLayout.removeAllViews();
        int count = countHowManyPages();
        for (int i = 0; i < count; i++) {
            //dot view
            ImageView mDotView = new ImageView(getContext());
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setSize(UiUtils.dp2px(6), UiUtils.dp2px(6));
            drawable.setColor(mUnSelectedColor);
            mDotView.setImageDrawable(drawable);
            int padding = UiUtils.dp2px(2);
            mDotView.setPadding(padding, padding, padding, padding);
            mIndicatorLayout.addView(mDotView);
        }
        mPrePosition = 0;
        mViewPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int position = mLayoutManager.findFirstVisibleItemPosition();
                    select(mPrePosition, position);
                    mPrePosition = position;
                }

            }
        });

        select(0, 0);
    }

    private void select(int prePosition, int nowPosition) {
        if (prePosition == nowPosition && prePosition == 0) {
            ((ImageView) mIndicatorLayout.getChildAt(nowPosition)).setColorFilter(mSelectedColor);
        } else {
            ((ImageView) mIndicatorLayout.getChildAt(prePosition)).setColorFilter(mUnSelectedColor);
            ((ImageView) mIndicatorLayout.getChildAt(nowPosition)).setColorFilter(mSelectedColor);
        }
    }

    private int countHowManyPages() {
        return (mAppInfoList.size() - 1) / PAGE_APPS_COUNT + 1;
    }


    private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            GridView view = new GridView(parent.getContext());
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mAdapter.setAdapterPosition(position);

        }

        @Override
        public int getItemCount() {
            return countHowManyPages();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            GridViewAdapter mAdapter;

            ViewHolder(View itemView) {
                super(itemView);
                GridView view = ((GridView) itemView);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                view.setLayoutParams(params);
                view.setFastScrollEnabled(false);
                view.setPadding(UiUtils.dp2px(20), 0, UiUtils.dp2px(20), 0);
                view.setNumColumns(4);
                view.setHorizontalSpacing(UiUtils.dp2px(10));
                view.setVerticalSpacing(UiUtils.dp2px(10));
                mAdapter = new GridViewAdapter();
                view.setAdapter(mAdapter);
            }
        }

        class GridViewAdapter extends BaseAdapter {

            private int adapterPosition;

            void setAdapterPosition(int adapterPosition) {
                this.adapterPosition = adapterPosition;
                notifyDataSetChanged();
            }

            @Override
            public int getCount() {
                if (adapterPosition != countHowManyPages() - 1) {
                    return PAGE_APPS_COUNT;
                } else {
                    return mAppInfoList.size() - PAGE_APPS_COUNT * (countHowManyPages() - 1);
                }
            }

            @Override
            public ShareAppInfo getItem(int position) {
                return mAppInfoList.get(adapterPosition * PAGE_APPS_COUNT + position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_share_single_app, parent, false);
                }
                ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
                imageView.setImageDrawable(getItem(position).getIcon());
                TextView textView = (TextView) convertView.findViewById(R.id.textView);
                textView.setText(getItem(position).getTitle());
                Paint.FontMetrics fontMetrics = textView.getPaint().getFontMetrics();
                float height = fontMetrics.bottom - fontMetrics.top;
                // two line height
                textView.setHeight((int) (2 * height));

                convertView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnAppClickListener != null) {
                            mOnAppClickListener.onClick(adapterPosition * PAGE_APPS_COUNT + position, getItem(position));
                        }
                    }
                });
                return convertView;
            }
        }

    }

}
