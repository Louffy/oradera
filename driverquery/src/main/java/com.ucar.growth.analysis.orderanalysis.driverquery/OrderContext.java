package com.ucar.growth.analysis.orderanalysis.driverquery;

import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverAction;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverSnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by zfx on 2016/8/25.
 */
public class OrderContext implements Serializable{
    public double lat;
    public double lon;
    public DriverSnapshot[] driverSnapshotArray;
    public ArrayList<TreeMap<Double,DriverSnapshot>> driverDistanceMap;
    //public HashMap<String,String> driverStatusMap;
    //public HashMap<String,Double[]> driverLoctionMap;
    public ArrayList<HashMap<String,ArrayList<DriverSnapshot>>> driverDistanceStatusMap;
    public double[][] driverPosSatArray;
    public String orderNo;
    public String time;
    public boolean canAdvance;

    public ArrayList<DriverAction> completeDriver;
    public ArrayList<DriverAction> completeDriver2;
    public ArrayList<DriverAction> completeDriver3;
    public HashMap<String,Double> statusCount;

    public TreeMap<Long,AdvanceDriver> advanceDriverMap;
    public TreeMap<Double,AdvanceDriver> advanceDriverDisMap;
    public Double[] disAvaiable;


    public ArrayList<HashMap<String, ArrayList<DriverSnapshot>>> getDriverDistanceStatusMap() {
        return driverDistanceStatusMap;
    }
    public int test;
    public OrderContext(){

    }

    public OrderContext(double lat, double lon, ArrayList<TreeMap<Double, DriverSnapshot>> driverDistanceMap, ArrayList<HashMap<String, ArrayList<DriverSnapshot>>> driverDistanceStatusMap, double[][] driverPosSatArray) {
        this.lat = lat;
        this.lon = lon;
        this.driverDistanceMap = driverDistanceMap;
        this.driverDistanceStatusMap = driverDistanceStatusMap;
        this.driverPosSatArray = driverPosSatArray;
    }

    public void setDriverDistanceStatusMap(ArrayList<HashMap<String, ArrayList<DriverSnapshot>>> driverDistanceStatusMap) {
        this.driverDistanceStatusMap = driverDistanceStatusMap;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public ArrayList<TreeMap<Double, DriverSnapshot>> getDriverDistanceMap() {
        return driverDistanceMap;
    }

    public void setDriverDistanceMap(ArrayList<TreeMap<Double, DriverSnapshot>> driverDistanceMap) {
        this.driverDistanceMap = driverDistanceMap;
    }

    public double[][] getDriverPosSatArray() {
        return driverPosSatArray;
    }

    public void setDriverPosSatArray(double[][] driverPosSatArray) {
        this.driverPosSatArray = driverPosSatArray;
    }



    public HashMap<String, Double> getStatusCount() {
        return statusCount;
    }

    public void setStatusCount(HashMap<String, Double> statusCount) {
        this.statusCount = statusCount;
    }

    public DriverSnapshot[] getDriverSnapshotArray() {
        return driverSnapshotArray;
    }

    public void setDriverSnapshotArray(DriverSnapshot[] driverSnapshotArray) {
        this.driverSnapshotArray = driverSnapshotArray;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public ArrayList<DriverAction> getCompleteDriver() {
        return completeDriver;
    }

    public void setCompleteDriver(ArrayList<DriverAction> completeDriver) {
        this.completeDriver = completeDriver;
    }

    public ArrayList<DriverAction> getCompleteDriver2() {
        return completeDriver2;
    }

    public void setCompleteDriver2(ArrayList<DriverAction> completeDriver2) {
        this.completeDriver2 = completeDriver2;
    }

    public ArrayList<DriverAction> getCompleteDriver3() {
        return completeDriver3;
    }

    public void setCompleteDriver3(ArrayList<DriverAction> completeDriver3) {
        this.completeDriver3 = completeDriver3;
    }






}
