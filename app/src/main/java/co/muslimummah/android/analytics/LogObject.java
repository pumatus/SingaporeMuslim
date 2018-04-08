package co.muslimummah.android.analytics;

import org.json.JSONException;
import org.json.JSONStringer;

/**
 * Created by Xingbo.Jie on 28/8/17.
 */

public class LogObject {
    private AnalyticsConstants.LOCATION location;
    private AnalyticsConstants.BEHAVIOUR behaviour;
    private AnalyticsConstants.TARGET_TYPE targetType;
    private String target;
    private String reservedParameter;
    private long ts;

    private LogObject(Builder builder) {
        location = builder.location;
        behaviour = builder.behaviour;
        targetType = builder.targetType;
        target = builder.target;
        reservedParameter = builder.reservedParameter;
        ts = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        JSONStringer root = new JSONStringer();
        try {
            root.object();
            root.key("ub").value(behaviour.value);

            if (location != null) {
                root.key("location").value(location.value);
            }

            root.key("t").value(ts);
            if (target != null && targetType != null) {
                root.key("tt").value(targetType.value);
                root.key("target").value(target);
            }

            if (reservedParameter != null) {
                root.key("rp").value(reservedParameter);
            }

            root.endObject();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return root.toString();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(LogObject copy) {
        Builder builder = new Builder();
        builder.location = copy.location;
        builder.behaviour = copy.behaviour;
        builder.targetType = copy.targetType;
        builder.target = copy.target;
        builder.reservedParameter = copy.reservedParameter;
        return builder;
    }

    public static final class Builder {
        private AnalyticsConstants.LOCATION location;
        private AnalyticsConstants.BEHAVIOUR behaviour;
        private AnalyticsConstants.TARGET_TYPE targetType;
        private String target;
        private String reservedParameter;

        private Builder() {
        }

        public Builder location(AnalyticsConstants.LOCATION location) {
            this.location = location;
            return this;
        }

        public Builder behaviour(AnalyticsConstants.BEHAVIOUR behaviour) {
            this.behaviour = behaviour;
            return this;
        }

        public Builder target(AnalyticsConstants.TARGET_TYPE targetType, String target) {
            this.targetType = targetType;
            this.target = target;
            return this;
        }

        public Builder reserved(String reservedParameter) {
            this.reservedParameter = reservedParameter;
            return this;
        }

        public LogObject build() {
            return new LogObject(this);
        }
    }
}
