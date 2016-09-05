package com.ucar.growth.analysis.orderanalysis.driverdata.Beans;

import java.io.Serializable;

/**
 * Created by zfx on 2016/8/24.
 */
public class DriverStatus implements Serializable{
    public DriverStatus(String dirverId, String time, DriverAction[] actionArray, String status, String availiable) {
        this.dirverId = dirverId;
        this.time = time;
        this.actionArray = actionArray;
        this.status = status;
        this.availiable = availiable;
    }

    public String getDirverId() {
        return dirverId;
    }

    public String getTime() {
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

    public String dirverId() {
        return dirverId;
    }

    public String time() {
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


    public String dirverId;
    public String time;
    public DriverAction[] actionArray;
    public String status;
    public String availiable;
}
