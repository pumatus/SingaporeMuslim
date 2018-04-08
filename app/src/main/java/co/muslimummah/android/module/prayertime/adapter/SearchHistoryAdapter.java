package co.muslimummah.android.module.prayertime.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import co.muslimummah.android.R;
import co.muslimummah.android.module.prayertime.data.model.SearchHistoryModel;
import co.muslimummah.android.module.prayertime.data.presenter.OnSearchHistoryListener;

public class SearchHistoryAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<SearchHistoryModel> mHistories;

    public SearchHistoryAdapter(Context context, ArrayList<SearchHistoryModel> histories) {
        mContext = context;
        mHistories = histories;
    }

    public void refreshData(ArrayList<SearchHistoryModel> histories) {
        mHistories.clear();
        mHistories = histories;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mHistories.size();
    }

    @Override
    public Object getItem(int position) {
        return mHistories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.home_listitem_search, null);
            holder = new ViewHolder();
            holder.textView = (TextView) convertView.findViewById(R.id.tv_searchhistory);
            holder.subTextView = (TextView) convertView.findViewById(R.id.tv_sub_content);
            holder.layoutClose = (ImageView) convertView.findViewById(R.id.iv_search_close);
            holder.layout = (LinearLayout) convertView.findViewById(R.id.ll_search_item);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textView.setText(mHistories.get(position).getContent());
        if (TextUtils.isEmpty(mHistories.get(position).getSubContent())) {
            holder.subTextView.setVisibility(View.GONE);
        } else {
            holder.subTextView.setVisibility(View.VISIBLE);
            holder.subTextView.setText(mHistories.get(position).getSubContent());
        }
        holder.layoutClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onSearchHistoryListener != null) {
                    onSearchHistoryListener.onDelete(mHistories.get(position));
                }
            }
        });
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onSearchHistoryListener != null) {
                    onSearchHistoryListener.onSelect(mHistories.get(position));
                }
            }
        });
        return convertView;
    }

    private static class ViewHolder {

        private LinearLayout layout;
        TextView textView;
        TextView subTextView;
        ImageView layoutClose;
    }

    public void setOnSearchHistoryListener(OnSearchHistoryListener onSearchHistoryListener) {
        this.onSearchHistoryListener = onSearchHistoryListener;
    }

    private OnSearchHistoryListener onSearchHistoryListener;
}
