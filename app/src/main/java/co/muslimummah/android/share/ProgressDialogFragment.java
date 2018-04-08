package co.muslimummah.android.share;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;

import co.muslimummah.android.R;

/**
 * Created by tysheng
 * Date: 4/10/17 1:40 PM.
 * Email: tyshengsx@gmail.com
 */

public class ProgressDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new MaterialDialog.Builder(getContext()).customView(R.layout.dialog_progress, false)
                .canceledOnTouchOutside(false).autoDismiss(false).build();
        return dialog;
    }
}
