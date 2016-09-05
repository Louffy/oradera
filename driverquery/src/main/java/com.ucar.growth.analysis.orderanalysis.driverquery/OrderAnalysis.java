package com.ucar.growth.analysis.orderanalysis.driverquery;

import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.*;
import com.ucar.growth.analysis.orderanalysis.driverdata.util.Util;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Created by zfx on 2016/8/24.
 */
public class OrderAnalysis {
    private static OrderAnalysis ourInstance = new OrderAnalysis();

    public static OrderAnalysis getInstance() {
        return ourInstance;
    }

    private OrderAnalysis() {
    }
    public static OrderContext initOC(OrderSnapshot orderSnapshot){
        OrderContext orderContext = new OrderContext();
        initBase(orderSnapshot,orderContext);
        initDistance(orderSnapshot,orderContext);
        orderContext.completeDriver = initCompleteStatus(orderContext,1000,180*1000);
        orderContext.completeDriver2 = initCompleteStatus(orderContext,1000,300*1000);
        orderContext.completeDriver3 = initCompleteStatus(orderContext,2000,300*1000);

        initAdvanceDriver(orderContext,3000,3*60*1000);

        return orderContext;

    }
    public static void initBase(OrderSnapshot orderSnapshot,OrderContext orderContext){
        orderContext.orderNo = orderSnapshot.getOrderId();
        orderContext.time = orderSnapshot.getDateTime();
        orderContext.lat = Double.valueOf(orderSnapshot.getLat());
        orderContext.lon = Double.valueOf(orderSnapshot.getLon());
        orderContext.driverSnapshotArray = orderSnapshot.getCitySnapshot().driverSnapshotArray;
    }
    public static void initDistance(OrderSnapshot orderSnapshot,OrderContext orderContext){
        ArrayList<TreeMap<Double,DriverSnapshot>> filterDriverSnapshot = new ArrayList<>();
        DriverSnapshot[] driverSnapshots = orderSnapshot.getCitySnapshot().getDriverSnapshotArray();
        TreeMap<Double,DriverSnapshot> distance5 = new TreeMap<>();
        TreeMap<Double,DriverSnapshot> distance10 = new TreeMap<>();
        TreeMap<Double,DriverSnapshot> distanceOther = new TreeMap<>();

        ArrayList<HashMap<String,ArrayList<DriverSnapshot>>> driverDistanceStatus = new ArrayList<>();
        HashMap<String,ArrayList<DriverSnapshot>> distance5Status = new HashMap<>();
        HashMap<String,ArrayList<DriverSnapshot>> distance10Status = new HashMap<>();
        HashMap<String,ArrayList<DriverSnapshot>> distanceOtherStatus = new HashMap<>();

        double[][] driverPosSta = new double[driverSnapshots.length][];

        HashMap<String,Double> statusCount = new HashMap<>();
        for(int i = 0;i<driverSnapshots.length;i++){
            DriverPosition driverPosition = driverSnapshots[i].getDriverPosition();

            Double distance = AnalysisUtil.calculateLineDistance(orderSnapshot.getLat(),orderSnapshot.getLon(),
                    driverPosition.getPd_lat(),driverPosition.getPd_lon());
            DriverStatus driverStatus = driverSnapshots[i].getDriverStatus();

            double[] driverPosStaTmp = {Double.valueOf(driverStatus.getAvailiable()),
                    Double.valueOf(driverPosition.getPd_lon()),
                    Double.valueOf(driverPosition.getPd_lat())};
            driverPosSta[i] = driverPosStaTmp;
            if(distance <= 5*1000) {
                distance5.put(distance, driverSnapshots[i]);
                if (distance5Status.containsKey(driverStatus.status))
                    distance5Status.get(driverStatus.status).add(driverSnapshots[i]);
                else{
                    ArrayList<DriverSnapshot> temp = new ArrayList<>();
                    temp.add(driverSnapshots[i]);
                    distance5Status.put(driverStatus.status, temp);
                }
            }
            else if(distance <=10*1000) {
                distance10.put(distance, driverSnapshots[i]);
                if (distance10Status.containsKey(driverStatus.status))
                    distance10Status.get(driverStatus.status).add(driverSnapshots[i]);
                else{
                    ArrayList<DriverSnapshot> temp = new ArrayList<>();
                    temp.add(driverSnapshots[i]);
                    distance10Status.put(driverStatus.status, temp);
                }
            }
            else {
                distanceOther.put(distance, driverSnapshots[i]);

                if (distanceOtherStatus.containsKey(driverStatus.status))
                    distanceOtherStatus.get(driverStatus.status).add(driverSnapshots[i]);
                else{
                    ArrayList<DriverSnapshot> temp = new ArrayList<>();
                    temp.add(driverSnapshots[i]);
                    distanceOtherStatus.put(driverStatus.status, temp);
                }
            }

            String status = driverStatus.getStatus();
            if(statusCount.containsKey(status)){
                statusCount.put(status,statusCount.get(status)+1);
            }else{
                statusCount.put(status,Double.valueOf(1));
            }

        }
        filterDriverSnapshot.add(distance5);
        filterDriverSnapshot.add(distance10);
        filterDriverSnapshot.add(distanceOther);
        orderContext.driverDistanceMap = filterDriverSnapshot;
        driverDistanceStatus.add(distance5Status);
        driverDistanceStatus.add(distance10Status);
        driverDistanceStatus.add(distanceOtherStatus);
        orderContext.driverDistanceStatusMap = driverDistanceStatus;

        orderContext.driverPosSatArray = driverPosSta;

        Iterator<String> stringIterator = statusCount.keySet().iterator();
        while(stringIterator.hasNext()){
            String key = stringIterator.next();
            statusCount.put(key,(Math.round((statusCount.get(key)/driverSnapshots.length)*10000)/10000.0));
        }
        orderContext.statusCount = statusCount;

    }
    public static ArrayList<DriverAction> initCompleteStatus(OrderContext orderContext,double dis,long g){
        TreeMap<Double,DriverSnapshot> driverSnapshotTreeMap = orderContext.driverDistanceMap.get(0);
        Iterator<Double> it = driverSnapshotTreeMap.keySet().iterator();
        ArrayList<DriverAction> driverActionTreeMap = new ArrayList<>();
        while(it.hasNext()){
            double d = it.next();
            DriverSnapshot driverSnapshot = driverSnapshotTreeMap.get(d);
            DriverAction[] driverActions = driverSnapshot.driverStatus().actionArray;
            if(driverActions.length<2)

                continue;
            if(driverActions[1]==null)
                continue;
            if(driverActions[1].getPd_oper_type().equals("服务结束")){
                double distance = AnalysisUtil.calculateLineDistance(String.valueOf(orderContext.lat),
                        String.valueOf(orderContext.lon),driverActions[1].getTd_lat(),driverActions[1].getTd_lon());
                long gap = Long.valueOf(driverActions[1].getKey())-Util.stringToTimestamp(orderContext.time);
                if(gap<=g&&distance<=dis)
                    driverActionTreeMap.add(driverActions[1]);

            }
        }
        return driverActionTreeMap;
    }
    //派单时刻，5公里以内的司机搜索，结束时刻g内，并且结束位置dis内
    public static void initAdvanceDriver(OrderContext orderContext,double dis,long g){
        TreeMap<Double,DriverSnapshot> driverSnapshotTreeMap = orderContext.driverDistanceMap.get(0);
        Iterator<Double> it = driverSnapshotTreeMap.keySet().iterator();
        TreeMap<Long,AdvanceDriver> advanceDrivers = new TreeMap<Long,AdvanceDriver>();
        TreeMap<Double,AdvanceDriver> advanceDriversDis = new TreeMap<>();
        while(it.hasNext()){
            double d = it.next();
            DriverSnapshot driverSnapshot = driverSnapshotTreeMap.get(d);
            DriverAction[] driverActions = driverSnapshot.driverStatus().actionArray;
            if(driverActions.length<2)
                continue;
            if(driverActions[1]==null)
                continue;
            if(driverActions[1].getPd_oper_type().equals("服务结束")){
                double distance = AnalysisUtil.calculateLineDistance(String.valueOf(orderContext.lat),
                        String.valueOf(orderContext.lon),driverActions[1].getTd_lat(),driverActions[1].getTd_lon());
                long gap = Long.valueOf(driverActions[1].getKey())-Util.stringToTimestamp(orderContext.time);
                if(gap<=g&&distance<=dis) {
                    String driverId = driverActions[1].getDriver_id();
                    long completeTime = Long.valueOf(driverActions[1].getKey());
                    ArrayList<DriverAction> driverActionArrayList = DB.getInstance().getAdvanceActions(driverId,driverActions[1].getKey());
                    if(driverActionArrayList == null)
                        continue;
                    driverActionArrayList.add(0,driverActions[1]);
                    AdvanceDriver advanceDriver = constructAdvanceDriver(orderContext,driverActionArrayList);
                    advanceDrivers.put(advanceDriver.advanceCompletePeriod,advanceDriver);
                    advanceDriversDis.put(advanceDriver.advanceBoardDistance,advanceDriver);
                }

            }
        }
        orderContext.advanceDriverMap = advanceDrivers;
        orderContext.advanceDriverDisMap = advanceDriversDis;
    }
    public static AdvanceDriver constructAdvanceDriver(OrderContext orderContext,ArrayList<DriverAction> driverActions){
        String driverId = driverActions.get(0).getDriver_id();
        long currentTime = Long.valueOf(Util.stringToTimestamp(orderContext.getTime()));

        long advanceCompletePeroid = Long.valueOf(driverActions.get(0).getKey()) - currentTime;

        double advanceDistance = AnalysisUtil.calculateLineDistance(String.valueOf(orderContext.lat),
                String.valueOf(orderContext.lon),driverActions.get(0).getTd_lat(),driverActions.get(0).getTd_lon());
        double historyDistance = AnalysisUtil.calculateLineDistance(driverActions.get(1).getTd_lat(),
                driverActions.get(1).getTd_lon(),driverActions.get(2).getTd_lat(),driverActions.get(2).getTd_lon());

        long historyTakeOrderPeriod = Long.valueOf(driverActions.get(1).getKey()) - Long.valueOf(driverActions.get(0).getKey());
        long historyArriveOrderPeriod = Long.valueOf(driverActions.get(2).getKey()) -
                Long.valueOf(driverActions.get(1).getKey());
        long historyAllPeriod = historyTakeOrderPeriod + historyArriveOrderPeriod;

        double historyOrderDistance = AnalysisUtil.calculateLineDistance(driverActions.get(2).getTd_lat(),
                driverActions.get(2).getTd_lon(),driverActions.get(0).getTd_lat(),driverActions.get(0).getTd_lon());
        double historyWaitorderDistance = AnalysisUtil.calculateLineDistance(driverActions.get(1).getTd_lat(),
                driverActions.get(1).getTd_lon(),driverActions.get(0).getTd_lat(),driverActions.get(0).getTd_lon());
        double speedDistance = AnalysisUtil.calculateLineDistance(driverActions.get(2).getTd_lat(),
                driverActions.get(2).getTd_lon(),driverActions.get(1).getTd_lat(),driverActions.get(1).getTd_lon());
        double speed = speedDistance/(historyArriveOrderPeriod/1000);
        //System.out.println("speed "+speed);
        long advanceBoardPeroid = (long)(advanceDistance/speed);
        return new AdvanceDriver(driverId,driverActions,advanceCompletePeroid,advanceBoardPeroid,
                advanceBoardPeroid+advanceCompletePeroid,historyTakeOrderPeriod,
                historyArriveOrderPeriod,historyAllPeriod,Long.valueOf(driverActions.get(0).getKey()),
                Long.valueOf(Util.stringToTimestamp(orderContext.getTime())),advanceDistance,historyDistance,
                historyOrderDistance,historyWaitorderDistance,speed);


    }
    public static HashMap<String,Double> countAdvanceDriver(ArrayList<OrderAdvance> orders,String flag){
        int orderSum1 = orders.size();
        int orderSum2 = 0;
        int advanceOrder = 0;
        int shortAdvanceOrder = 0;
        int advanceOrderDriver = 0;
        double advanceDisAvg = 0;
        double historyDisAvg = 0;
        double advanceCompletePeriodAvg = 0;
        double advanceBoardPeriodAvg = 0;
        double historyWaitOrder = 0;
        for(int i = 0;i<orders.size();i++){
            orderSum2++;
            //System.out.println(orders[i]);
            OrderAdvance OrderAdvance = orders.get(i);
            if(OrderAdvance.advanceDriverMap.size() == 0)
                continue;
            advanceOrder++;
            AdvanceDriver advanceDriver = null;
            if(flag.equals("period"))
                advanceDriver = OrderAdvance.advanceDriverMap.firstEntry().getValue();
            else if(flag.equals("distance"))
                advanceDriver = OrderAdvance.advanceDriverDisMap.firstEntry().getValue();
            //System.out.println(advanceDriver.getHistoryTakeOrderPeriod()/1000);
            //System.out.println((int)advanceDriver.advanceBoardDistance + ":"+(int)advanceDriver.historyBoardDistance);
            advanceOrderDriver += OrderAdvance.advanceDriverMap.size();
            historyWaitOrder += advanceDriver.historyTakeOrderPeriod;
            if(advanceDriver.advanceBoardDistance<advanceDriver.historyBoardDistance)
                shortAdvanceOrder++;
            advanceDisAvg += advanceDriver.advanceBoardDistance;
            historyDisAvg += advanceDriver.historyBoardDistance;
            advanceCompletePeriodAvg += advanceDriver.advanceCompletePeriod;
            advanceBoardPeriodAvg += advanceDriver.advanceBoardPeriod;

        }
        HashMap<String,Double> result = new HashMap<>();
        result.put("orderSum1",(double)orderSum1);
        result.put("orderSum2",(double)orderSum2);
        result.put("advanceOrder",(double)advanceOrder);
        result.put("shortAdvanceOrder",(double)shortAdvanceOrder);

        result.put("advanceDisAvg",advanceDisAvg/advanceOrder);
        result.put("historyDisAvg",historyDisAvg/advanceOrder);
        result.put("advanceCompletePeriodAvg",advanceCompletePeriodAvg/advanceOrder);
        result.put("advanceBoardPeriodAvg",advanceBoardPeriodAvg/advanceOrder);
        result.put("historyWaitOrder",historyWaitOrder/advanceOrder);
        result.put("advanceOrderDriver",(double)advanceOrderDriver*1.0/advanceOrder);

        return result;

    }
    public static ArrayList<ArrayList<OrderAdvance>> getHourOrder(String[] orders){
        ArrayList<ArrayList<OrderAdvance>> orderList = new ArrayList<>();
        for(int i = 0;i<24;i++){
            ArrayList<OrderAdvance> arrayList = new ArrayList<>();
            orderList.add(arrayList);
        }
        for(int i = 0;i<orders.length;i++) {
            //if(i%57!=0)
            //    continue;
            if(i%197!=0)
                continue;

            OrderSnapshot orderSnapshot = DriverQuery.getOrderSnapshot2(orders[i]);
            if(orderSnapshot == null)
                continue;
            System.out.println(orders[i]);
            OrderContext orderContext = OrderAnalysis.initOC(orderSnapshot);
            OrderAdvance orderAdvance = new OrderAdvance(orderContext.orderNo,orderSnapshot.getDateTime(),
                    orderSnapshot.getLat(),orderSnapshot.getLon(),
                    orderContext.advanceDriverMap,orderContext.advanceDriverDisMap);
            DateTime dateTime = Util.timestampToDateTime(Util.stringToTimestampms(orderSnapshot.getDateTime()));
            int hour = dateTime.getHourOfDay();
            System.out.println(hour);
            orderList.get(hour).add(orderAdvance);
        }
        return orderList;

    }
    public static void main(String[] args) {
        String[] orders = DB.getInstance().getInvalidOrderList();

        ArrayList<ArrayList<OrderAdvance>> orderList = getHourOrder(orders);
        for(int i = 0;i<orderList.size();i++){
            HashMap<String,Double> result1 = countAdvanceDriver(orderList.get(i),"period");
            HashMap<String,Double> result2 = countAdvanceDriver(orderList.get(i),"distance");
            System.out.println(result1.toString());
            System.out.println(result2.toString());
        }
        OrderSnapshot orderSnapshot = DriverQuery.getOrderSnapshot2("o27306447357232");
        OrderContext orderContext = OrderAnalysis.initOC(orderSnapshot);
        System.out.println(orderContext.advanceDriverDisMap.size());

    }
}
