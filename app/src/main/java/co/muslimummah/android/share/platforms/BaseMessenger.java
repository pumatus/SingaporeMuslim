package co.muslimummah.android.share.platforms;

import android.content.Intent;
import android.text.TextUtils;

import co.muslimummah.android.share.ShareMessage;
import co.muslimummah.android.share.ShareUtils;

/**
 * Created by tysheng
 * Date: 12/10/17 8:18 PM.
 * Email: tyshengsx@gmail.com
 */

public class BaseMessenger extends BasePlatform {
    public BaseMessenger(ShareMessage shareMessage) {
        super(shareMessage);
        setImageUrl(shareMessage.getImageUrl());
        setText2(shareMessage.getText2());
    }

    @Override
    public Intent convert(Intent intent) {
        if (!TextUtils.isEmpty(getImageUrl())) {
            intent.setType(IMAGE);

            intent.putExtra(Intent.EXTRA_STREAM, ShareUtils.downloadUrlToUri(getVideoUrl(), true, isAddWatermark()));
        } else {
            intent.setType(TEXT);
            intent.putExtra(Intent.EXTRA_TEXT, getText2());
        }
        return intent;
    }
}
