package com.ucar.growth.analysis.orderanalysis.driverquery;

import com.google.gson.*;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.*;
import com.ucar.growth.analysis.orderanalysis.driverdata.util.JavaUtil;
import org.apache.hadoop.hive.ql.exec.vector.mapjoin.fast.VectorMapJoinFastValueStore;
import org.apache.hadoop.util.hash.Hash;
import org.joda.time.DateTime;
import scala.math.Ordering;

import java.io.*;
import java.lang.reflect.Array;
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
        Double[] disAvaiable = new Double[2];
        int dis5in = 0;
        int dis5out = 0;
        int dis5inAll = 0;
        int dis5outAll = 0;

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

            if(check5in(driverPosition)) {
                dis5inAll++;
                if (driverStatus.getAvailiable().equals("1"))
                    dis5in++;
            }
            else{
                dis5outAll++;
                if(driverStatus.getAvailiable().equals("1"))
                    dis5out++;
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

        disAvaiable[0] = dis5in*1.0/(dis5inAll+dis5outAll);
        disAvaiable[1] = dis5out*1.0/(dis5inAll+dis5outAll);
        orderContext.disAvaiable = disAvaiable;

    }
    public static boolean check5in(DriverPosition driverPosition){
        double lat = Double.valueOf(driverPosition.getPd_lat());
        double lon = Double.valueOf(driverPosition.getPd_lon());
        if(lat < Double.valueOf("39.983454") && lat > Double.valueOf("39.829916")
                && lon < Double.valueOf("116.489782") && lon > Double.valueOf("116.274862"))
            return true;
        else
            return false;
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
                    orderContext.canAdvance = true;
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

        int orderSum = 0;
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
        long canAdvance = 0;
        double avaiable5in = 0;
        double avaiable5out = 0;

        double available = 0;

        for(int i = 0;i<orders.size();i++){
            orderSum++;

            //System.out.println(orders[i]);
            OrderAdvance orderAdvance = orders.get(i);
            available += orderAdvance.available;
            avaiable5in += orderAdvance.available5in;
            avaiable5out += orderAdvance.avaiable5out;
            if(orderAdvance.canAdv)
                canAdvance++;
            if(orderAdvance.advanceDriverMap.size() == 0)
                continue;
            advanceOrder++;

            AdvanceDriver advanceDriver = null;
            if(flag.equals("period"))
                advanceDriver = orderAdvance.advanceDriverMap.firstEntry().getValue();
            else if(flag.equals("distance"))
                advanceDriver = orderAdvance.advanceDriverDisMap.firstEntry().getValue();
            else if(flag.equals("all")){
                advanceDriver = averageOrderAdvance(orderAdvance.advanceDriverDisMap);
            }
            //System.out.println(advanceDriver.getHistoryTakeOrderPeriod()/1000);
            //System.out.println((int)advanceDriver.advanceBoardDistance + ":"+(int)advanceDriver.historyBoardDistance);
            advanceOrderDriver += orderAdvance.advanceDriverMap.size();
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

        result.put("orderSum",(double)orderSum);

        result.put("advanceOrder",(double)advanceOrder);
        result.put("shortAdvanceOrder",(double)shortAdvanceOrder);

        if(orderSum == 0) {
            result.put("CanAdvance", 0D);
            result.put("available", 0D);
            result.put("available5in",0D);
            result.put("available5out",0D);
        }
        else{
            result.put("CanAdvance",canAdvance*1.0/orderSum);
            result.put("available",available/orderSum);
            result.put("available5in",avaiable5in/orderSum);
            result.put("available5out",avaiable5out/orderSum);
            System.out.println("======================" + advanceOrder + " " + orderSum + " " + advanceOrder * 1.0 / orderSum);
        }
        if(advanceOrder == 0)
            result.put("ShortAdvance",(double)-1);
        else {
            result.put("ShortAdvance", shortAdvanceOrder * 1.0 / advanceOrder);
            System.out.println("======================" + shortAdvanceOrder + " " + orderSum + " " + shortAdvanceOrder * 1.0 / orderSum);
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
            double available = 0;
            if(orderContext.getStatusCount() != null)
                if(orderContext.getStatusCount().containsKey("空闲"))
                    available = orderContext.getStatusCount().get("空闲");

            double available5in = 0;
            double available5out = 0;
            if(orderContext.disAvaiable != null) {
                if (orderContext.disAvaiable.length > 0)
                    available5in = orderContext.disAvaiable[0];
                if (orderContext.disAvaiable.length > 1)
                    available5out = orderContext.disAvaiable[1];
            }

            OrderAdvance orderAdvance = new OrderAdvance(orderContext.orderNo,available,available5in,available5out,
                    orderContext.canAdvance,orderSnapshot.getDateTime(),
                    orderSnapshot.getLat(), orderSnapshot.getLon(),
                    orderContext.advanceDriverMap, orderContext.advanceDriverDisMap);

            orderAdvance.canAdv = orderContext.canAdvance;

            DateTime dateTime = JavaUtil.timestampToDateTime(JavaUtil.stringToTimestampms(orderSnapshot.getDateTime()));
            int hour = dateTime.getHourOfDay();
            System.out.println(hour);
            orderList.get(hour).add(orderAdvance);
        }
        return orderList;

    }

    /**
     *
     * @param gap
     * @param day
     * @return
     */
    public static HashMap<String,HashMap<String,Double>> countAll(int gap,String day){
        String[] orders = DB.getInstance().getInvalidOrderList(day);
        HashMap<String,HashMap<String,Double>> all= new HashMap<>();
        ArrayList<ArrayList<OrderAdvance>> orderList = getHourOrder(orders,gap);
        for(int i = 0;i<orderList.size();i++){
           // HashMap<String,Double> result1 = countAdvanceDriver(orderList.get(i),"period");
            HashMap<String,Double> result2 = countAdvanceDriver(orderList.get(i),"distance");
           // HashMap<String,Double> result3 = countAdvanceDriver(orderList.get(i),"all");

            System.out.println("d---"+result2.toString());

            all.put(i+"distance",result2);
        }
        return all;
    }
    public static HashMap<String,Double[]> allToArray(HashMap<String ,HashMap<String,Double>> all){
        HashMap<String,Double[]> result = new HashMap<>();
        DecimalFormat df = new DecimalFormat("0.00");
        df.setRoundingMode(RoundingMode.HALF_UP);
        for(int i = 0;i<24;i++){
           // HashMap<String,Double> r1 = all.get(i+"period");
            HashMap<String,Double> r2 = all.get(i+"distance");
          //  HashMap<String,Double> r3 = all.get(i+"all");
            Iterator<String> it = r2.keySet().iterator();
            while(it.hasNext()){
                String key = it.next();
                if(!result.containsKey(key)) {
                    result.put(key, new Double[24]);
                }

              //  result.get(key)[0][i] = Double.valueOf(df.format(r1.get(key)));
                System.out.println(key);
                System.out.println("qqqqqq"+r2.get(key));
                result.get(key)[i] = Double.valueOf(df.format(r2.get(key)));

            }

        }
        return result;

    }
    public static void write(String file,HashMap<String,Double[]> r){
        try {
            File f = new File(file);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f,false));
            Iterator<Map.Entry<String,Double[]>> it = r.entrySet().iterator();
            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            while (it.hasNext()){
                Map.Entry<String,Double[]> entry = it.next();
                System.out.println(entry.getValue().length);
                String key = entry.getKey();
                Double[] value = entry.getValue();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("key",key);
                JsonElement jsonArray = gson.toJsonTree(value,Double[].class);
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
    public static HashMap<String,Double[]> read(String file){
        try{
            File f = new File(file);
            BufferedReader br = new BufferedReader(new FileReader(f));
            HashMap<String,Double[]> re = new HashMap<>();
            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            JsonParser jp = new JsonParser();
            String line = null;
            while((line = br.readLine())!= null){
                JsonObject je = jp.parse(line).getAsJsonObject();
                System.out.println(je.get("key"));
                System.out.println(je.get("value"));
                String key = gson.fromJson(je.get("key"),String.class);
                Double[] value = gson.fromJson(je.getAsJsonArray("value"),Double[].class);
                Double[] temp = Arrays.copyOfRange(value,6,22);
                re.put(key,temp);
            }

            Double[] data = new Double[16];
            for (int i = 0; i < data.length; i++) {
                data[i] = re.get("orderSum")[i]/10000;
            }
            re.put("allOrderSum",data);
            return re;
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
    public static void gen(String filename,String day,int gap){
        HashMap<String,HashMap<String,Double>> all = countAll(gap,day);
        HashMap<String,Double[]> r = allToArray(all);
        write(filename,r);
    }
    public static void compute(HashMap<String,Double[]> re){
        HashMap<String,Double> count = new HashMap<>();
        Iterator<String> it = re.keySet().iterator();
        while(it.hasNext()){
            String key = it.next();
            Double[] value = re.get(key);
            double com = 0;

                double sum = 0;
                double sumO = 0;
                for (int i = 0; i < value.length; i++)
                    sum += value[i];
                com = sum / value.length;
                for (int k = 0; k < value.length; k++) {
                    sumO += value[k];
                }
                System.out.println(key+" "+com);

            count.put(key,com);
        }
        System.out.println(new Gson().toJson(count));
    }
    public static void genOneDay(String day,int gap){
        gen("result/count"+day,day,gap);
    }
    public static void genOneDayAverage(String day){
        HashMap<String,Double[]> re = read("result/count"+day);
        compute(re);
    }
    public static void main(String[] args) {
        String day1 = "0801I";
        String day2 = "0801S";
        String day3 = "0817I";
        String day4 = "0817S";
        String day = "0817";
        String day0801 = "0801";
        int gap = 1;
        genOneDay(day1,gap);
        genOneDay(day2,gap);
       //genOneDayAverage(day);
        genOneDay(day0801,gap);
        //genOneDay(day4,gap);
       //genOneDay(day,gap);
        //genOneDay(day0801,gap);
        //genOneDayAverage(day);
        //genOneDayAverage(day3);
        //genOneDayAverage(day1);
        //genOneDay(day4,gap);


    }
}
