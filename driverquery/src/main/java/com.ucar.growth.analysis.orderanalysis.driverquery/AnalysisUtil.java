package com.ucar.growth.analysis.orderanalysis.driverquery;

/**
 * Created by zfx on 2016/8/24.
 */
public class AnalysisUtil {
    // distance(meter) of two lat lon,return meters
    public static Double calculateLineDistance(String lats,String lons,String late,String lone){

        double R = 12742001.579854401;
        double P = 0.01745329251994329;
        double lon1 = P * Double.valueOf(lons);
        double lat1 = P * Double.valueOf(lats);
        double lon2 = P * Double.valueOf(lone);
        double lat2 = P * Double.valueOf(late);

        double d1 = Math.sin(lon1);
        double d2 = Math.sin(lat1);
        double d3 = Math.cos(lon1);
        double d4 = Math.cos(lat1);
        double d5 = Math.sin(lon2);
        double d6 = Math.sin(lat2);
        double d7 = Math.cos(lon2);
        double d8 = Math.cos(lat2);
        double tmpresult = Math.sqrt((d4 * d3 - d8 * d7) *
                (d4 * d3 - d8 * d7) + (d4 * d1 - d8 * d5) *
                (d4 * d1 - d8 * d5) + (d2 - d6) * (d2 - d6));
        double result = Math.asin(tmpresult/ 2.0) * R;
        return result;
    }

}
