package com.ucar.growth.analysis.orderanalysis.driverdata.model;

import java.io.Serializable;

/**
 * Created by zfx on 2016/8/23.
 */
public class DriverPosition implements Serializable{


    private long time;

    private String pd_lat;

    private String pd_lon;
    private int driver_id;

    public DriverPosition(int driver_id, long time, String pd_lat, String pd_lon) {
        this.driver_id = driver_id;
        this.time = time;
        this.pd_lat = pd_lat;
        this.pd_lon = pd_lon;
    }



    public String pd_lat() {
        return pd_lat;
    }

    public String pd_lon() {
        return pd_lon;
    }
    public long getTime() {
        return time;
    }

    public String getPd_lat() {
        return pd_lat;
    }

    public String getPd_lon() {
        return pd_lon;
    }
}
