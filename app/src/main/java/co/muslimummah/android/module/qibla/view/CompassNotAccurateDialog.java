package co.muslimummah.android.module.qibla.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import co.muslimummah.android.R;

/**
 * Created by Hongd on 2017/8/21.
 */

public class CompassNotAccurateDialog extends Dialog {

    private Context context;

    public CompassNotAccurateDialog(@NonNull Context context) {
        this(context, R.style.dialog_highlight);
    }

    public CompassNotAccurateDialog(Context context, int theme) {
        super(context, theme);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_highlight, null);
        this.setContentView(view);
    }
}
