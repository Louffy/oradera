package com.ucar.growth.analysis.orderanalysis.driverdata.model;

import java.io.Serializable;

/**
 * Created by zfx on 2016/8/24.
 */
public class DriverStatus implements Serializable{
    public DriverStatus(int dirverId, long time, DriverAction[] actionArray, String status, String availiable) {
        this.dirverId = dirverId;
        this.time = time;
        this.actionArray = actionArray;
        this.status = status;
        this.availiable = availiable;
    }

    public int getDirverId() {
        return dirverId;
    }

    public long getTime() {
        return time;
    }

    public DriverAction[] getActionArray() {
        return actionArray;
    }

    public String getStatus() {
        return status;
    }

    public String getAvailiable() {
        return availiable;
    }

    public int dirverId() {
        return dirverId;
    }

    public long time() {
        return time;
    }

    public DriverAction[] actionArray() {
        return actionArray;
    }

    public String status() {
        return status;
    }

    public String availiable() {
        return availiable;
    }


    public int dirverId;
    public long time;
    public DriverAction[] actionArray;
    public String status;
    public String availiable;
}
