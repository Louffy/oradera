package com.ucar.growth.analysis.orderanalysis.driverquery;

import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverAction;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by zfx on 2016/9/2.
 */
public class AdvanceDriver implements Serializable{
    public String driverId;
    //low,服务结束，接单，到达
    public ArrayList<DriverAction> contextActions;
    //服务结束时间
    public long advanceCompletePeriod;


    //
    public long advanceBoardPeriod;
    public long advanceWaitPeriod;
    //历史接单时间
    public long historyTakeOrderPeriod;
    public long historyArriveOrderPeriod;
    public long historyAllPeriod;
    public long completeTime;
    public long currentTime;
    //服务结束到达提前订单距离
    public double advanceBoardDistance;
    //服务结束到达历史订单距离
    public double historyBoardDistance;



    public double historyOrderDistance;
    public double historyWaitorderDistance;

    public long currentSpeed;

    public AdvanceDriver(String driverId, ArrayList<DriverAction> contextActions,
                         long advanceCompletePeriod, long advanceBoardPeriod,
                         long advanceWaitPeriod, long historyTakeOrderPeriod,
                         long historyArriveOrderPeriod, long historyAllPeriod,
                         long completeTime, long currentTime,
                         double advanceBoardDistance, double historyBoardDistance,
                         double historyOrderDistance, double historyWaitorderDistance,
                         long currentSpeed, long shortDriver) {
        this.driverId = driverId;
        this.contextActions = contextActions;
        this.advanceCompletePeriod = advanceCompletePeriod;
        this.advanceBoardPeriod = advanceBoardPeriod;
        this.advanceWaitPeriod = advanceWaitPeriod;
        this.historyTakeOrderPeriod = historyTakeOrderPeriod;
        this.historyArriveOrderPeriod = historyArriveOrderPeriod;
        this.historyAllPeriod = historyAllPeriod;
        this.completeTime = completeTime;
        this.currentTime = currentTime;
        this.advanceBoardDistance = advanceBoardDistance;
        this.historyBoardDistance = historyBoardDistance;
        this.historyOrderDistance = historyOrderDistance;
        this.historyWaitorderDistance = historyWaitorderDistance;
        this.currentSpeed = currentSpeed;
        this.shortDriver = shortDriver;
    }

    public long shortDriver;




    public long getAdvanceBoardPeriod() {
        return advanceBoardPeriod;
    }

    public void setAdvanceBoardPeriod(long advanceBoardPeriod) {
        this.advanceBoardPeriod = advanceBoardPeriod;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public ArrayList<DriverAction> getContextActions() {
        return contextActions;
    }

    public void setContextActions(ArrayList<DriverAction> contextActions) {
        this.contextActions = contextActions;
    }

    public long getAdvanceCompletePeriod() {
        return advanceCompletePeriod;
    }

    public void setAdvanceCompletePeriod(long advanceCompletePeriod) {
        this.advanceCompletePeriod = advanceCompletePeriod;
    }

    public long getAdvanceWaitPeriod() {
        return advanceWaitPeriod;
    }

    public void setAdvanceWaitPeriod(long advanceWaitPeriod) {
        this.advanceWaitPeriod = advanceWaitPeriod;
    }

    public long getHistoryTakeOrderPeriod() {
        return historyTakeOrderPeriod;
    }

    public void setHistoryTakeOrderPeriod(long historyTakeOrderPeriod) {
        this.historyTakeOrderPeriod = historyTakeOrderPeriod;
    }

    public long getHistoryArriveOrderPeriod() {
        return historyArriveOrderPeriod;
    }

    public void setHistoryArriveOrderPeriod(long historyArriveOrderPeriod) {
        this.historyArriveOrderPeriod = historyArriveOrderPeriod;
    }

    public long getHistoryAllPeriod() {
        return historyAllPeriod;
    }

    public void setHistoryAllPeriod(long historyAllPeriod) {
        this.historyAllPeriod = historyAllPeriod;
    }

    public long getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(long completeTime) {
        this.completeTime = completeTime;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public double getAdvanceBoardDistance() {
        return advanceBoardDistance;
    }

    public void setAdvanceBoardDistance(double advanceBoardDistance) {
        this.advanceBoardDistance = advanceBoardDistance;
    }

    public double getHistoryBoardDistance() {
        return historyBoardDistance;
    }

    public void setHistoryBoardDistance(double historyBoardDistance) {
        this.historyBoardDistance = historyBoardDistance;
    }

    public double getHistoryOrderDistance() {
        return historyOrderDistance;
    }

    public void setHistoryOrderDistance(double historyOrderDistance) {
        this.historyOrderDistance = historyOrderDistance;
    }

    public double getHistoryWaitorderDistance() {
        return historyWaitorderDistance;
    }

    public void setHistoryWaitorderDistance(double historyWaitorderDistance) {
        this.historyWaitorderDistance = historyWaitorderDistance;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }







}
