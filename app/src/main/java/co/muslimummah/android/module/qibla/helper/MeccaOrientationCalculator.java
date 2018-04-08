package co.muslimummah.android.module.qibla.helper;

/**
 * Created by frank on 9/27/17.
 */
public class MeccaOrientationCalculator {
    private static final double MECCA_LAT = 21.422491;
    private static final double MECCA_LNG = 39.826209;

    /**
     * @return Degree of the direction from the input location to the location of Mecca.
     */
    public static float computeToMeccaRadian(double lat, double lng) {
        return (float) Math.atan2(Math.sin(Math.toRadians(MECCA_LNG - lng)),
                Math.cos(Math.toRadians(lat)) * Math.tan(Math.toRadians(MECCA_LAT))
                        - Math.sin(Math.toRadians(lat)) * Math.cos(Math.toRadians(MECCA_LNG - lng)));
    }

    public static float computeToMeccaDegree(double lat, double lng) {
        return (float) Math.toDegrees(computeToMeccaRadian(lat, lng));
    }
}
