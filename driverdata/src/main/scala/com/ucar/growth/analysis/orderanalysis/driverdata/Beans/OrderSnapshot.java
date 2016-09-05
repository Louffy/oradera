package com.ucar.growth.analysis.orderanalysis.driverdata.Beans;

/**
 * Created by zfx on 2016/8/24.
 */
public class OrderSnapshot {
    public OrderSnapshot(){

    }
    public OrderSnapshot(String orderId, String dateTime, CitySnapshot citySnapshot, String lat, String lon) {
        this.orderId = orderId;
        this.dateTime = dateTime;
        this.citySnapshot = citySnapshot;
        this.lat = lat;
        this.lon = lon;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public CitySnapshot getCitySnapshot() {
        return citySnapshot;
    }

    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }

    private String orderId;
    private String dateTime;
    private CitySnapshot citySnapshot;

    public OrderSnapshot(String orderId, String dateTime, String lat, String lon) {
        this.orderId = orderId;
        this.dateTime = dateTime;
        this.lat = lat;
        this.lon = lon;
    }

    private String lat;
    private String lon;
}
