package co.muslimummah.android.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import co.muslimummah.android.R;
import lombok.Builder;
import lombok.Data;

/**
 * Created by Xingbo.Jie on 9/10/17.
 */

public class SingleChoiceDialog {

    @Data
    @Builder
    public static class Params {
        String title;
        List<Item> items;
        int selecedItem;
        String positiveButton;
        String negativeButton;
        DialogInterface.OnDismissListener onDismissListener;
        DialogInterface.OnClickListener itemClickListener;
        OnPositiveButtonClickListener positiveButtonClickListener;
        DialogInterface.OnClickListener negativeButtonClickListener;
    }


    public interface OnPositiveButtonClickListener {
        void onClick(int selectedItem);
    }

    public static AlertDialog create(Context context, final Params params) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        if (!TextUtils.isEmpty(params.title)) {
            builder.setTitle(params.title);
        }

        final Adapter adapter = new Adapter(params.items, params.selecedItem, params.itemClickListener);
        builder.setSingleChoiceItems(adapter, params.selecedItem, adapter);

        if (!TextUtils.isEmpty(params.positiveButton)) {
            builder.setPositiveButton(params.positiveButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (params.positiveButtonClickListener != null) {
                        params.positiveButtonClickListener.onClick(adapter.selectedItem);
                    }
                }
            });
        }

        if (!TextUtils.isEmpty(params.negativeButton)) {
            builder.setNegativeButton(params.negativeButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (params.negativeButtonClickListener != null) {
                        params.negativeButtonClickListener.onClick(dialog, which);
                    }
                }
            });
        }

        builder.setOnDismissListener(params.onDismissListener);

        AlertDialog dialog = builder.create();
        dialog.getListView().setDivider(new ColorDrawable(0xffeeeeee));
        dialog.getListView().setDividerHeight(1);
        return dialog;
    }

    @Data
    @Builder
    public static class Item {
        private int icon;
        private String text;
    }

    public static class Adapter extends BaseAdapter implements DialogInterface.OnClickListener {
        private List<Item> items;
        private DialogInterface.OnClickListener itemClickListener;
        int selectedItem;


        public Adapter(List<Item> items, int selectedItem, DialogInterface.OnClickListener itemClickListener) {
            this.items = items;
            this.selectedItem = selectedItem;
            this.itemClickListener = itemClickListener;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_single_choice_dialog, parent, false);
            }

            ImageView icon = convertView.findViewById(R.id.icon);
            icon.setImageResource(items.get(position).getIcon());
            icon.setSelected(selectedItem == position);

            TextView text = convertView.findViewById(R.id.text);
            text.setText(items.get(position).getText());

            convertView.findViewById(R.id.selected).setSelected(selectedItem == position);
            return convertView;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (selectedItem != which) {
                selectedItem = which;
                notifyDataSetChanged();
            }

            if (itemClickListener != null) {
                itemClickListener.onClick(dialog, which);
            }
        }
    }
}
