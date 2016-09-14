package com.ucar.growth.analysis.orderanalysis.driverquery;

import com.google.gson.*;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.*;
import com.ucar.growth.analysis.orderanalysis.driverdata.util.JavaUtil;
import org.apache.hadoop.hive.ql.exec.vector.mapjoin.fast.VectorMapJoinFastValueStore;
import org.apache.hadoop.util.hash.Hash;
import org.joda.time.DateTime;
import scala.math.Ordering;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

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
                long gap = Long.valueOf(driverActions[1].getKey())- JavaUtil.stringToTimestamp(orderContext.time);
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
                long gap = Long.valueOf(driverActions[1].getKey())- JavaUtil.stringToTimestamp(orderContext.time);
                if(gap<=g&&distance<=dis) {
                    String driverId = driverActions[1].getDriver_id();
                    long completeTime = Long.valueOf(driverActions[1].getKey());
                    ArrayList<DriverAction> driverActionArrayList = DB.getInstance().getAdvanceActions(driverId,driverActions[1].getKey());
                    if(driverActionArrayList == null)
                        continue;
                    driverActionArrayList.add(0,driverActions[1]);
                    AdvanceDriver advanceDriver = constructAdvanceDriver(orderContext,driverActionArrayList);
                    if(advanceDriver != null) {
                        advanceDrivers.put(advanceDriver.advanceCompletePeriod, advanceDriver);
                        advanceDriversDis.put(advanceDriver.advanceBoardDistance, advanceDriver);
                    }
                }

            }
        }
        orderContext.advanceDriverMap = advanceDrivers;
        orderContext.advanceDriverDisMap = advanceDriversDis;
    }
    public static AdvanceDriver constructAdvanceDriver(OrderContext orderContext,ArrayList<DriverAction> driverActions){
        String driverId = driverActions.get(0).getDriver_id();
        long currentTime = Long.valueOf(JavaUtil.stringToTimestamp(orderContext.getTime()));

        long advanceCompletePeroid = (Long.valueOf(driverActions.get(0).getKey()) - currentTime);

        double advanceDistance = AnalysisUtil.calculateLineDistance(String.valueOf(orderContext.lat),
                String.valueOf(orderContext.lon),driverActions.get(0).getTd_lat(),driverActions.get(0).getTd_lon());
        double historyDistance = AnalysisUtil.calculateLineDistance(driverActions.get(1).getTd_lat(),
                driverActions.get(1).getTd_lon(),driverActions.get(2).getTd_lat(),driverActions.get(2).getTd_lon());

        long historyTakeOrderPeriod = (Long.valueOf(driverActions.get(1).getKey()) -
                Long.valueOf(driverActions.get(0).getKey()));
        long historyArriveOrderPeriod = (Long.valueOf(driverActions.get(2).getKey()) -
                Long.valueOf(driverActions.get(1).getKey()));
        long historyAllPeriod = (historyTakeOrderPeriod + historyArriveOrderPeriod);

        double historyOrderDistance = AnalysisUtil.calculateLineDistance(driverActions.get(2).getTd_lat(),
                driverActions.get(2).getTd_lon(),driverActions.get(0).getTd_lat(),driverActions.get(0).getTd_lon());
        double historyWaitorderDistance = AnalysisUtil.calculateLineDistance(driverActions.get(1).getTd_lat(),
                driverActions.get(1).getTd_lon(),driverActions.get(0).getTd_lat(),driverActions.get(0).getTd_lon());
        double speedDistance = AnalysisUtil.calculateLineDistance(driverActions.get(2).getTd_lat(),
                driverActions.get(2).getTd_lon(),driverActions.get(1).getTd_lat(),driverActions.get(1).getTd_lon());
        long speed = (long)(1000*60*60*speedDistance/(historyArriveOrderPeriod));
        if(speed < 7000){
            speed = 7000;
        }

        long advanceBoardPeroid = 0;
        if(speed != 0)
            advanceBoardPeroid = (long)(advanceDistance*3600.0*1000/speed);
        System.out.println("speed :"+speed+"  "+advanceBoardPeroid);
        long shortDriver = 0;
        if(advanceDistance < historyOrderDistance)
            shortDriver = 1;
        return new AdvanceDriver(driverId,driverActions,advanceCompletePeroid/1000,advanceBoardPeroid/1000,
                (advanceBoardPeroid+advanceCompletePeroid)/1000,historyTakeOrderPeriod/1000,
                historyArriveOrderPeriod/1000,historyAllPeriod/1000,Long.valueOf(driverActions.get(0).getKey()),
                Long.valueOf(JavaUtil.stringToTimestamp(orderContext.getTime())),advanceDistance,historyDistance,
                historyOrderDistance,historyWaitorderDistance,speed,shortDriver);


    }
    public static AdvanceDriver averageOrderAdvance(TreeMap<Double,AdvanceDriver> advanceDriverTreeMap){
        String driverId = "AVG";
        long currentTime = -1;

        long advanceCompletePeroid = 0L;

        double advanceDistance = 0;
        double historyBoardDistance = 0;

        long historyTakeOrderPeriod = 0L;
        long historyArriveOrderPeriod = 0L;
        long historyAllPeriod = 0L;

        double historyOrderDistance = 0;
        double historyWaitorderDistance = 0;

        long speed = 0;
        long shortDriver = 0;


        long advanceBoardPeroid = 0L;
        Iterator<Double> it = advanceDriverTreeMap.keySet().iterator();
        long boardPeroid = 0;
        long speed0  = 0;
        long len = 0;
        while(it.hasNext()){
            AdvanceDriver advanceDriver = advanceDriverTreeMap.get(it.next());
            advanceCompletePeroid += advanceDriver.advanceCompletePeriod;
            advanceDistance += advanceDriver.advanceBoardDistance;
            historyBoardDistance += advanceDriver.historyBoardDistance;
            historyTakeOrderPeriod += advanceDriver.historyTakeOrderPeriod;
            historyArriveOrderPeriod += advanceDriver.historyArriveOrderPeriod;
            historyAllPeriod += advanceDriver.historyAllPeriod;
            historyOrderDistance += advanceDriver.historyOrderDistance;
            historyWaitorderDistance += advanceDriver.historyWaitorderDistance;
            speed += advanceDriver.currentSpeed;

            shortDriver += advanceDriver.shortDriver;

            advanceBoardPeroid += advanceDriver.advanceBoardPeriod;

            len++;
            if(len==5)
                break;

        }
        if(len == 0)
            len = Integer.MAX_VALUE;
        return new AdvanceDriver(driverId,null,
                (long)(advanceCompletePeroid*1.0/len),
                (long)(advanceBoardPeroid*1.0/(len)),
                (long)(advanceBoardPeroid*1.0/(len)+advanceCompletePeroid*1.0/len),
                (long)(historyTakeOrderPeriod*1.0/len),
                (long)(historyArriveOrderPeriod*1.0/(len)),
                (long)(historyAllPeriod*1.0/len),
                -1,-1,
                advanceDistance*1.0/len,
                historyBoardDistance*1.0/len,
                historyOrderDistance*1.0/len,
                historyWaitorderDistance*1.0/len,
                (long)(speed*1.0/(len)),
                shortDriver);
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
        double historyBoardPeriodAvg = 0;
        double historyWaitOrder = 0;
        long speed = 0;
        double historyAllPeriodAvg = 0;
        double advanceWaitPeriodAvg = 0;
        double historyOrderDistance = 0;
        long shortDriver = 0;
        long historyAllPeriod = 0;
        long adp0 = 0;
        long advanceWaitPeriod = 0;
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
            else if(flag.equals("all")){
                advanceDriver = averageOrderAdvance(OrderAdvance.advanceDriverDisMap);
            }
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

            historyBoardPeriodAvg += advanceDriver.historyArriveOrderPeriod;


            System.out.println(advanceDriver.advanceBoardDistance+" "+advanceDriver.currentSpeed);
            System.out.println(advanceBoardPeriodAvg);
            speed += advanceDriver.currentSpeed;
            historyAllPeriodAvg += advanceDriver.historyAllPeriod;
            advanceWaitPeriodAvg += advanceDriver.advanceWaitPeriod;
            historyOrderDistance += advanceDriver.historyOrderDistance;
            shortDriver += advanceDriver.shortDriver;


        }
        HashMap<String,Double> result = new HashMap<>();
        result.put("orderSum1",(double)orderSum1);
        result.put("orderSum2",(double)orderSum2);
        result.put("advanceOrder",(double)advanceOrder);
        result.put("shortAdvanceOrder",(double)shortAdvanceOrder);

        if(orderSum2 == 0)
            result.put("CanAdvance",(double)-1);
        else{
            result.put("CanAdvance",advanceOrder*1.0/orderSum2);
            System.out.println("======================" + advanceOrder + " " + orderSum2 + " " + advanceOrder * 1.0 / orderSum2);
        }
        if(advanceOrder == 0)
            result.put("ShortAdvance",(double)-1);
        else {
            result.put("ShortAdvance", shortAdvanceOrder * 1.0 / advanceOrder);
            System.out.println("======================" + shortAdvanceOrder + " " + orderSum2 + " " + shortAdvanceOrder * 1.0 / orderSum2);
        }

        if(advanceOrder == 0)
            advanceOrder = Integer.MAX_VALUE;

        result.put("advanceDisAvg",advanceDisAvg/advanceOrder);
        result.put("historyDisAvg",historyDisAvg/advanceOrder);
        result.put("advanceCompletePeriodAvg",advanceCompletePeriodAvg/advanceOrder);
        result.put("advanceBoardPeriodAvg",advanceBoardPeriodAvg/(advanceOrder));
        result.put("historyWaitOrder",historyWaitOrder/advanceOrder);
        result.put("advanceOrderDriver",(double)advanceOrderDriver*1.0/advanceOrder);
        result.put("speed",(double)speed*1.0/advanceOrder);
        result.put("historyBoardPeriodAvg",historyBoardPeriodAvg/advanceOrder);
        result.put("historyAllPeriodAvg",historyAllPeriodAvg/advanceOrder);
        result.put("advanceWaitPeriodAvg",advanceWaitPeriodAvg/advanceOrder);
        result.put("historyOrderDistance",historyOrderDistance/advanceOrder);
        result.put("shortDriver",(double)shortDriver*1.0/advanceOrder);

        return result;

    }
    public static ArrayList<ArrayList<OrderAdvance>> getHourOrder(String[] orders,int gap) {
        ArrayList<ArrayList<OrderAdvance>> orderList = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            ArrayList<OrderAdvance> arrayList = new ArrayList<>();
            orderList.add(arrayList);
        }
        for (int i = 0; i < orders.length; i++) {
            //if(i%57!=0)
            //    continue;
            System.out.println(i);
            if (i % gap != 0)
                continue;

            OrderSnapshot orderSnapshot = DriverQuery.getOrderSnapshot2(orders[i]);
            if (orderSnapshot == null)
                continue;
            System.out.println(orders[i]);
            OrderContext orderContext = OrderAnalysis.initOC(orderSnapshot);
            OrderAdvance orderAdvance = new OrderAdvance(orderContext.orderNo, orderSnapshot.getDateTime(),
                    orderSnapshot.getLat(), orderSnapshot.getLon(),
                    orderContext.advanceDriverMap, orderContext.advanceDriverDisMap);
            DateTime dateTime = JavaUtil.timestampToDateTime(JavaUtil.stringToTimestampms(orderSnapshot.getDateTime()));
            int hour = dateTime.getHourOfDay();
            System.out.println(hour);
            orderList.get(hour).add(orderAdvance);
        }
        return orderList;

    }
    public static HashMap<String,HashMap<String,Double>> countAll(int gap,String day){
        String[] orders = DB.getInstance().getInvalidOrderList(day);
        HashMap<String,HashMap<String,Double>> all= new HashMap<>();
        ArrayList<ArrayList<OrderAdvance>> orderList = getHourOrder(orders,gap);
        for(int i = 0;i<orderList.size();i++){
            HashMap<String,Double> result1 = countAdvanceDriver(orderList.get(i),"period");
            HashMap<String,Double> result2 = countAdvanceDriver(orderList.get(i),"distance");
            HashMap<String,Double> result3 = countAdvanceDriver(orderList.get(i),"all");
            System.out.println("p---"+result1.toString());
            System.out.println("d---"+result2.toString());
            System.out.println("a---"+result3.toString());
            all.put(i+"period",result1);
            all.put(i+"distance",result2);
            all.put(i+"all",result3);
        }
        return all;
    }
    public static HashMap<String,Double[][]> allToArray(HashMap<String ,HashMap<String,Double>> all){
        HashMap<String,Double[][]> result = new HashMap<>();
        DecimalFormat df = new DecimalFormat("0.00");
        df.setRoundingMode(RoundingMode.HALF_UP);
        for(int i = 0;i<24;i++){
            HashMap<String,Double> r1 = all.get(i+"period");
            HashMap<String,Double> r2 = all.get(i+"distance");
            HashMap<String,Double> r3 = all.get(i+"all");
            Iterator<String> it = r1.keySet().iterator();
            while(it.hasNext()){
                String key = it.next();
                if(!result.containsKey(key)) {
                    result.put(key, new Double[3][24]);
                }
                result.get(key)[0][i] = Double.valueOf(df.format(r1.get(key)));
                result.get(key)[1][i] = Double.valueOf(df.format(r2.get(key)));
                System.out.println(key);
                System.out.println("qqqqqqqqqqq"+r3.get(key));

                result.get(key)[2][i] = Double.valueOf(df.format(r3.get(key)));
                System.out.println("qqqqqqqqqqq"+result.get(key)[2][i]);
            }

        }
        return result;

    }
    public static void write(String file,HashMap<String,Double[][]> r){
        try {
            File f = new File(file);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f,false));
            Iterator<Map.Entry<String,Double[][]>> it = r.entrySet().iterator();
            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            while (it.hasNext()){
                Map.Entry<String,Double[][]> entry = it.next();
                System.out.println(entry.getValue().length);
                String key = entry.getKey();
                Double[][] value = entry.getValue();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("key",key);
                JsonElement jsonArray = gson.toJsonTree(value,Double[][].class);
                jsonObject.add("value", jsonArray);
                String src = gson.toJson(jsonObject);
                bw.write(src);
                bw.write("\n");
            }
            bw.flush();
            bw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public static HashMap<String,Double[][]> read(String file){
        try{
            File f = new File(file);
            BufferedReader br = new BufferedReader(new FileReader(f));
            HashMap<String,Double[][]> re = new HashMap<>();
            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            JsonParser jp = new JsonParser();
            String line = null;
            while((line = br.readLine())!= null){
                JsonObject je = jp.parse(line).getAsJsonObject();
                System.out.println(je.get("key"));
                System.out.println(je.get("value"));
                String key = gson.fromJson(je.get("key"),String.class);
                Double[][] value = gson.fromJson(je.getAsJsonArray("value"),Double[][].class);
                re.put(key,value);
            }
            return re;
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
    public static void gen(String filename,String day,int gap){
        HashMap<String,HashMap<String,Double>> all = countAll(gap,day);
        HashMap<String,Double[][]> r = allToArray(all);
        write(filename,r);
    }
    public static void compute(HashMap<String,Double[][]> re){
        HashMap<String,Double[]> count = new HashMap<>();
        Iterator<String> it = re.keySet().iterator();
        while(it.hasNext()){
            String key = it.next();
            Double[][] value = re.get(key);
            Double[] com = new Double[3];
            for(int j = 0;j<3;j++) {
                double sum = 0;
                double sumO = 0;
                for (int i = 6; i < 23; i++)
                    sum += value[j][i];
                com[j] = sum / 17;
                for (int k = 6; k < 23; k++) {
                    sumO += value[j][k];
                }
                System.out.println(key+" "+sumO);
            }
            count.put(key,com);
        }
        System.out.println(new Gson().toJson(count));
    }
    public static void genOneDay(String day,int gap){
        gen("result/count"+day,day,gap);
        HashMap<String,Double[][]> re = read("result/count"+day);
        compute(re);
    }
    public static void main(String[] args) {
        String day1 = "0801";
        String day2 = "0801S";
        String day3 = "0817";
        String day4 = "0817S";
        int gap = 17;
        //genOneDay(day1,gap);
        //genOneDay(day2,gap);
        genOneDay(day3,gap);
        //genOneDay(day4,gap);

    }
}
