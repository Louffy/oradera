package com.ucar.growth.analysis.orderanalysis.driverquery;

import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.*;
import com.ucar.growth.analysis.orderanalysis.driverdata.util.JavaUtil;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by zfx on 2016/8/24.
 */
public class DriverQuery {

    public static final DB db = DB.getInstance();
    public static DriverPosition getPosition(String driverId,String timeStamp){
        return db.getLocation(driverId,timeStamp);
    }
    public static DriverAction[] getAction(String driverId,String timeStamp){
        return db.getAction(driverId, timeStamp);
    }
    public static DriverAction[] getDriverAction(String driverId){
        return db.getActionList(driverId);
    }
    public static DriverSnapshot getSnapshot(String driverId,String timeStamp){
        String dateTime = JavaUtil.timestampToDateTime(Long.valueOf(timeStamp)).toString();
        return new DriverSnapshot(driverId, dateTime,
                constructDriverStatus(driverId,dateTime,getAction(driverId,timeStamp)),
                getPosition(driverId,timeStamp));
    }

    private static DriverStatus constructDriverStatus(String driverId, String dateTime, DriverAction[] driverActionArray){
        String status = checkDriverStatus(driverActionArray,dateTime);
        int availiable = 0;
        if(status.equals("空闲"))
            availiable = 1;
        else if(status.equals("下线"))
            availiable = 2;
        else if(status.equals("服务中-取消"))
            availiable = 3;
        else if(status.equals("小休"))
            availiable = 4;
        else if(status.equals("服务中-预约"))
            availiable = 5;
        return new DriverStatus(driverId,dateTime,driverActionArray,status,String.valueOf(availiable));
    }
    private static String checkDriverStatus(DriverAction[] driverAction,String timeStamp){

        if(driverAction.length<2)
            return "下线";
        DriverAction a1 = driverAction[0];
        DriverAction a2 = driverAction[1];
        if(a1 == null || a2 == null)
            return "下线";

        if(a1.getDriver_id().compareTo(a2.getDriver_id())!=0)
            return "下线";

        String s1 = a1.getPd_oper_type();
        String s2 = a2.getPd_oper_type();

        if(s2.equals("接单")){
            if(s1.equals("接单")||s1.equals("出发")||s1.equals("到达"))
                return "服务中-取消";
            else if(s1.equals("开始服务"))
                return "服务中-开始服务";
            else
                return "空闲";
        }
        else if(s1.equals("更新费用")){
            if((Long.valueOf(timeStamp)-Long.valueOf(driverAction[0].getKey()))<30*1000)
                return "服务中-更新";
        }
        else if(s1.equals("服务结束")){
            if((Long.valueOf(timeStamp)-Long.valueOf(driverAction[0].getKey()))>5*60*1000)
                return "空闲";
        }
       else if(s1.equals("更新费用")||s1.equals("开启自动接橙单")||s1.equals("开启自动接蓝单")||s1.equals("开启接蓝单")) {
            if (s2.equals("出发"))
                return "服务中-预约";
            else
                return "空闲";
        }
       else if(s1.equals("上班-正常上班")) {
           //重新登录
           if(s2.equals("出发")||s2.equals("到达")||s2.equals("开始服务")||s2.equals("服务结束")||s2.equals("更新费用"))
               return "服务中-重新登录-"+s1+"-"+s2;
            else
               return "空闲";
        }
        else if(s1.equals("退出")&&s2.equals("登录"))
            return "下线";
        else if(s1.equals("下班-临时小休")&&s2.equals("上班-正常上班"))
            return "小休";
        else
            return "服务中-"+s1;
        return "服务中-"+s1;

    }

    public static DriverSnapshot[] getSnapshotList(String timeStamp){
        HashMap<String,DriverPosition> locList = db.getLocationList(timeStamp);
        HashMap<String,DriverPosition> locList2 = db.getLocationList(String.valueOf(Long.valueOf(timeStamp)-60000));
        Iterator<String> locit2 = locList2.keySet().iterator();
        while(locit2.hasNext()){
            String key = locit2.next();
            if(locList.containsKey(key))
                continue;
            else{
                locList.put(key,locList2.get(key));
            }
        }
        String[] driverIdList = locList.keySet().toArray(new String[locList.size()]);
        DriverSnapshot[] dslist = new DriverSnapshot[driverIdList.length];
        String dateTime = JavaUtil.timestampToDateTime(Long.valueOf(timeStamp)).toString();
        for (int i = 0;i<driverIdList.length;i++) {
            DriverAction[] da = getAction(driverIdList[i],timeStamp);
            DriverStatus ds = constructDriverStatus(driverIdList[i],timeStamp,da);
            dslist[i] = new DriverSnapshot(driverIdList[i],dateTime,ds,
                    locList.get(driverIdList[i]));

        }
        return dslist;
    }
    public static CitySnapshot getCitySnapshot(String cityId,String timeStamp){
        DriverSnapshot[] driverList = getSnapshotList(timeStamp);
        return new CitySnapshot(cityId,timeStamp,driverList);

    }
    public static OrderSnapshot getOrderSnapshot(String timeStamp,String lat,String lon){
        DriverSnapshot[] driverList = getSnapshotList(timeStamp);
        return new OrderSnapshot("0",timeStamp,getCitySnapshot("1",timeStamp),lat,lon);
    }
    public static OrderSnapshot getOrderSnapshot(String orderNo,String day){
        OrderSnapshot orderSnapshot = db.getInvalidOrder(orderNo,day);
        String timeStamp =String.valueOf(
                JavaUtil.stringToTimestampms(orderSnapshot.getDateTime()));
        return new OrderSnapshot(orderSnapshot.getOrderId(),orderSnapshot.getDateTime(),getCitySnapshot("1",timeStamp),
                orderSnapshot.getLat(),orderSnapshot.getLon());
    }
    public static OrderSnapshot getOrderSnapshot2(String orderNo){
        OrderSnapshot orderSnapshot = db.getInvalidOrder2(orderNo);
        String timeStamp =String.valueOf(
                JavaUtil.stringToTimestampms(orderSnapshot.getDateTime()));
        return new OrderSnapshot(orderSnapshot.getOrderId(),orderSnapshot.getDateTime(),getCitySnapshot("1",timeStamp),
                orderSnapshot.getLat(),orderSnapshot.getLon());
    }
}
