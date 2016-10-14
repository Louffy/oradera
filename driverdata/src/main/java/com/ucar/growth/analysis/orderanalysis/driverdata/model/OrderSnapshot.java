package com.ucar.growth.analysis.orderanalysis.driverdata.model;

/**
 * Created by zfx on 2016/8/24.
 */
public class OrderSnapshot {
    public OrderSnapshot(){

    }

    public String getOrderId() {
        return orderId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }
    public int getCityId() {
        return cityId;
    }

    public OrderSnapshot(String orderId, String dateTime, int ciytId, String lat, String lon) {
        this.orderId = orderId;
        this.dateTime = dateTime;
        this.cityId = ciytId;
        this.lat = lat;
        this.lon = lon;
    }

    private String orderId;
    private String dateTime;



    private String lat;
    private String lon;


    private int cityId;
}
