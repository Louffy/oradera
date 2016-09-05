package com.ucar.growth.analysis.orderanalysis.driverquery;

import java.util.TreeMap;

/**
 * Created by zfx on 2016/9/5.
 */
public class OrderAdvance {
    public OrderAdvance(String orderNo, String timeStamp, String lat, String lon, TreeMap<Long, AdvanceDriver> advanceDriverMap, TreeMap<Double, AdvanceDriver> advanceDriverDisMap) {
        this.orderNo = orderNo;
        this.timeStamp = timeStamp;
        this.lat = lat;
        this.lon = lon;
        this.advanceDriverMap = advanceDriverMap;
        this.advanceDriverDisMap = advanceDriverDisMap;
    }

    public String orderNo;

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public TreeMap<Long, AdvanceDriver> getAdvanceDriverMap() {
        return advanceDriverMap;
    }

    public void setAdvanceDriverMap(TreeMap<Long, AdvanceDriver> advanceDriverMap) {
        this.advanceDriverMap = advanceDriverMap;
    }

    public TreeMap<Double, AdvanceDriver> getAdvanceDriverDisMap() {
        return advanceDriverDisMap;
    }

    public void setAdvanceDriverDisMap(TreeMap<Double, AdvanceDriver> advanceDriverDisMap) {
        this.advanceDriverDisMap = advanceDriverDisMap;
    }

    public String timeStamp;
    public String lat;
    public String lon;
    public TreeMap<Long,AdvanceDriver> advanceDriverMap;
    public TreeMap<Double,AdvanceDriver> advanceDriverDisMap;
}
