package com.ucar.growth.analysis.orderanalysis.driverdata.Beans;

import java.io.Serializable;

/**
 * Created by zfx on 2016/8/24.
 */
public class DriverSnapshot implements Serializable {
    public DriverSnapshot(String driverId, String dateTime, DriverStatus driverStatus, DriverPosition driverPosition) {
        this.driverId = driverId;
        this.dateTime = dateTime;
        this.driverStatus = driverStatus;
        this.driverPosition = driverPosition;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public DriverStatus getDriverStatus() {
        return driverStatus;
    }

    public DriverPosition getDriverPosition() {
        return driverPosition;
    }

    public String driverId() {
        return driverId;
    }

    public String dateTime() {
        return dateTime;
    }

    public DriverStatus driverStatus() {
        return driverStatus;
    }

    public DriverPosition driverPosition() {
        return driverPosition;
    }



    private String driverId;
    private String dateTime;

    private DriverStatus driverStatus;
    private DriverPosition driverPosition;


}
