package com.ucar.growth.analysis.orderanalysis.driverdata.model;

import java.io.Serializable;

/**
 * Created by zfx on 2016/8/24.
 */
public class DriverSnapshot implements Serializable {
    public DriverSnapshot(int driverId, long dateTime, DriverStatus driverStatus, DriverPosition driverPosition) {
        this.driverId = driverId;
        this.dateTime = dateTime;
        this.driverStatus = driverStatus;
        this.driverPosition = driverPosition;
    }

    public int getDriverId() {
        return driverId;
    }

    public long getDateTime() {
        return dateTime;
    }

    public DriverStatus getDriverStatus() {
        return driverStatus;
    }

    public DriverPosition getDriverPosition() {
        return driverPosition;
    }

    public int driverId() {
        return driverId;
    }

    public long dateTime() {
        return dateTime;
    }

    public DriverStatus driverStatus() {
        return driverStatus;
    }

    public DriverPosition driverPosition() {
        return driverPosition;
    }



    private int driverId;
    private long dateTime;

    private DriverStatus driverStatus;
    private DriverPosition driverPosition;


}
