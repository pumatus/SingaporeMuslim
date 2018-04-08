package co.muslimummah.android.share;

import android.graphics.drawable.Drawable;

import java.io.Serializable;


/**
 * Created by tysheng
 * Date: 2/10/17 6:58 PM.
 * Email: tyshengsx@gmail.com
 */

public class ShareAppInfo implements Serializable {
    private String title;
    private String packageName;
    private Drawable icon;
    private String activityName;
    private Float index;

    public float getIndex() {
        if (index == null) {
            return 1000f;
        }
        return index;
    }

    public void setIndex(Float index) {
        this.index = index;
    }

    public ShareAppInfo() {
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

}
