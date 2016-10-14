package com.ucar.growth.analysis.orderanalysis.driverdata.model;

import java.io.Serializable;

/**
 * Created by zfx on 2016/8/24.
 */
public class CitySnapshot implements Serializable {
    public CitySnapshot(String cityId, String dateTime, DriverSnapshot[] driverSnapshotArray) {
        this.cityId = cityId;
        this.dateTime = dateTime;
        this.driverSnapshotArray = driverSnapshotArray;
    }

    public String getCityId() {
        return cityId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public DriverSnapshot[] getDriverSnapshotArray() {
        return driverSnapshotArray;
    }

    public String cityId() {
        return cityId;
    }

    public String dateTime() {
        return dateTime;
    }

    public DriverSnapshot[] driverSnapshotArray() {
        return driverSnapshotArray;
    }

    public String cityId;
    public String dateTime;
    public DriverSnapshot[] driverSnapshotArray;
}
