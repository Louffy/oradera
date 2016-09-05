package com.ucar.growth.analysis.orderanalysis.driverdata.Beans;

import java.io.Serializable;

/**
 * Created by zfx on 2016/8/23.
 */
public class DriverPosition implements Serializable{


    private String key;

    private String pd_lat;

    private String pd_lon;

    public DriverPosition(String key, String pd_lat, String pd_lon) {
        this.key = key;
        this.pd_lat = pd_lat;
        this.pd_lon = pd_lon;
    }

    public String key() {
        return key;
    }

    public String pd_lat() {
        return pd_lat;
    }

    public String pd_lon() {
        return pd_lon;
    }
    public String getKey() {
        return key;
    }

    public String getPd_lat() {
        return pd_lat;
    }

    public String getPd_lon() {
        return pd_lon;
    }
}
