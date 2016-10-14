package com.ucar.growth.analysis.orderanalysis.driverdata.model;

import java.io.Serializable;

/**
 * Created by zfx on 2016/8/24.
 */
public class DriverAction implements Serializable {
    public long getAction_time() {
        return action_time;
    }

    public int getDriver_id() {
        return driver_id;
    }

    public String getPd_oper_type() {
        return pd_oper_type;
    }

    public String getTd_lat() {
        return td_lat;
    }

    public String getTd_lon() {
        return td_lon;
    }



    public long action_time(){
        return action_time;
    }
    public int driver_id(){
        return driver_id;
    }
    public String pd_oper_type(){
        return pd_oper_type;
    }
    public String td_lat(){
        return td_lat;
    }
    public String td_lon(){
        return td_lon;
    }
    public DriverAction(int driver_id,long time,  String pd_oper_type, String td_lat, String td_lon, String pd_remark) {
        this.driver_id = driver_id;
        this.action_time = time;
        this.pd_oper_type = pd_oper_type;
        this.td_lat = td_lat;
        this.td_lon = td_lon;
        this.pd_remark = pd_remark;

    }

    public String getPd_remark() {
        return pd_remark;
    }
    public String pd_remark(){
        return pd_remark;
    }

    private long action_time;
    private int driver_id;
    private String pd_oper_type;

    private String td_lat;
    private String td_lon;
    private String pd_remark;

}
