package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.support.v7.app.AppCompatDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.R;

/**
 * Created by frank on 8/16/17.
 */

public class DownloadStateDialog extends AppCompatDialog {
    @BindView(R.id.iv_network_state)
    ImageView ivNetworkState;
    @BindView(R.id.tv_content)
    TextView tvContent;
    @BindView(R.id.btn_bottom)
    Button btnBottom;

    public DownloadStateDialog(Context context) {
        this(context, 0);
    }

    public DownloadStateDialog(Context context, int theme) {
        super(context, theme);
        init(context);
    }

    protected DownloadStateDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init(context);
    }

    private void init(Context context) {
        setContentView(R.layout.layout_download_state_dialog);
        ButterKnife.bind(this);
    }

    public void setContent(String content) {
        tvContent.setText(content);
    }

    public void setButtonText(String text) {
        if (TextUtils.isEmpty(text)) {
            btnBottom.setVisibility(View.GONE);
        } else {
            btnBottom.setText(text);
            btnBottom.setVisibility(View.VISIBLE);
        }
    }

    public void setButtonEnabled(boolean enabled) {
        btnBottom.setEnabled(enabled);
    }

    public void setBottomButtonOnClickListener(View.OnClickListener onClickListener) {
        btnBottom.setOnClickListener(onClickListener);
    }

    public void setNetworkStateEnabled(boolean enabled) {
        ivNetworkState.setSelected(enabled);
    }
}
