package co.muslimummah.android.module.qibla.view;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.R;
import co.muslimummah.android.module.qibla.helper.MeccaOrientationCalculator;

/**
 * Created by frank on 9/26/17.
 */

public class OrientationInfoView extends RelativeLayout {
    @BindView(R.id.tv_angle)
    TextView tvAngle;
    @BindView(R.id.tv_orientation)
    TextView tvOrientation;
    @BindView(R.id.tv_latitude)
    TextView tvLatitude;
    @BindView(R.id.tv_longitude)
    TextView tvLongitude;

    public OrientationInfoView(Context context) {
        this(context, null);
    }

    public OrientationInfoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OrientationInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.layout_orientation_info_view, this);
        ButterKnife.bind(this);

        if (isInEditMode()) {
            //Display info for Jakarta in layout editor.
            setDisplayLocation(-6.21462, 106.84513, MeccaOrientationCalculator.computeToMeccaDegree(-6.21462, 106.84513));
        }
    }

    public void setDisplayLocation(double lat, double lng, float meccaDegree) {
        tvAngle.setText(String.format(Locale.US, "%d°", (Math.round(meccaDegree) + 360) % 360));
        tvOrientation.setText(processOrientationDisplay(Math.round(meccaDegree)));

        tvLatitude.setText(processLocationDisplay(Location.convert(lat, Location.FORMAT_SECONDS), true));
        tvLongitude.setText(processLocationDisplay(Location.convert(lng, Location.FORMAT_SECONDS), false));
    }
    private String getString(int id){
        return getContext().getString(id);
    }
    private String processOrientationDisplay(int degree) {
        if (degree > 0) {
            if (degree < 90) {
                return getString(R.string.north_east);
            }
            if (degree == 90) {
                return getString(R.string.east);
            }
            if (degree < 180) {
                return getString(R.string.south_east);
            }
            return getString(R.string.south);
        } else if (degree < 0) {
            if (degree > -90) {
                return getString(R.string.north_west);
            }
            if (degree == -90) {
                return getString(R.string.west);
            }
            if (degree > -180) {
                return getString(R.string.south_west);
            }
            return getString(R.string.south);
        } else {
            return getString(R.string.north);
        }
    }

    private String processLocationDisplay(String convertedLocationStr, boolean forLatitude) {
        boolean isNegative = convertedLocationStr.startsWith("-");
        String[] degreeMinuteSecondList = TextUtils.split(convertedLocationStr, ":");
        int degree = Math.abs(Integer.valueOf(degreeMinuteSecondList[0]));
        int minute = Integer.valueOf(degreeMinuteSecondList[1]);
        int second = 0;
        try {
            DecimalFormat decimalFormat = new DecimalFormat("##.####");
            second = (int) Math.round(decimalFormat.parse(degreeMinuteSecondList[2]).doubleValue());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String direction = forLatitude ? (isNegative ? getString(R.string.south) : getString(R.string.north)) : (isNegative ? getString(R.string.west) : getString(R.string.east));
        return String.format(Locale.US, "%d° %d' %d\" %s", degree, minute, second, direction);
    }
}
