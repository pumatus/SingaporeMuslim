package co.muslimummah.android.network.Entity.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Xingbo.Jie on 19/9/17.
 */

public class TimeZoneJson implements Serializable {
    /**
     * dstOffset : 0
     * rawOffset : 28800
     * status : OK
     * timeZoneName : Singapore Standard Time
     */

    @SerializedName("dstOffset")
    private int dstOffset;
    @SerializedName("rawOffset")
    private int rawOffset;
    @SerializedName("status")
    private String status;
    @SerializedName("timeZoneName")
    private String timeZoneName;

    /**
     * timeZoneId : Asia/Singapore
     */
    @SerializedName("timeZoneId")
    private String timeZoneId;

    public int getDstOffset() {
        return dstOffset;
    }

    public void setDstOffset(int dstOffset) {
        this.dstOffset = dstOffset;
    }

    public int getRawOffset() {
        return rawOffset;
    }

    public void setRawOffset(int rawOffset) {
        this.rawOffset = rawOffset;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimeZoneName() {
        return timeZoneName;
    }

    public void setTimeZoneName(String timeZoneName) {
        this.timeZoneName = timeZoneName;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }
}
