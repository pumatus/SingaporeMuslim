package co.muslimummah.android.share.platforms;

import android.content.Intent;

import co.muslimummah.android.share.ShareMessage;

/**
 * Created by tysheng
 * Date: 12/10/17 8:17 PM.
 * Email: tyshengsx@gmail.com
 */

public class Line extends BasePlatform {
    public Line(ShareMessage shareMessage) {
        super(shareMessage);
        setText2(shareMessage.getText2());
    }

    @Override
    public Intent convert(Intent intent) {

        intent.setType(TEXT);
        intent.putExtra(Intent.EXTRA_TEXT, getText2());


        return intent;
    }
}
