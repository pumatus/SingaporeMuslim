package co.muslimummah.android.share.platforms;

import android.content.Intent;
import android.text.TextUtils;

import co.muslimummah.android.share.ShareMessage;
import co.muslimummah.android.share.ShareUtils;

/**
 * Created by tysheng
 * Date: 11/10/17 7:27 PM.
 * Email: tyshengsx@gmail.com
 */

public class Twitter extends BasePlatform {
    public Twitter(ShareMessage shareMessage) {
        super(shareMessage);
        setText1(shareMessage.getText1());
        setText2(shareMessage.getText2());
        setVideoUrl(shareMessage.getVideoUrl());
    }

    @Override
    public Intent convert(Intent intent) {
        if (!TextUtils.isEmpty(getVideoUrl())) {
            intent.setType(VIDEO);
            intent.putExtra(Intent.EXTRA_TEXT, getText1());
            intent.putExtra(Intent.EXTRA_STREAM, ShareUtils.downloadUrlToUri(getVideoUrl(), true, isAddWatermark()));
        } else {
            intent.setType(TEXT);
            intent.putExtra(Intent.EXTRA_TEXT, getText2());
        }
        return intent;
    }
}
