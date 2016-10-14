package com.ucar.growth.analysis.orderanalysis.driverdata.query;

import com.google.gson.*;
import com.ucar.growth.analysis.orderanalysis.driverdata.model.*;
import com.ucar.growth.analysis.orderanalysis.driverdata.util.*;

import java.io.*;
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
    public static OrderContext initOC(OrderSnapshot orderSnapshot,DriverSnapshot[] driverSnapshotArray){
        OrderContext orderContext = new OrderContext();
        initBase(orderSnapshot,driverSnapshotArray,orderContext);
        initDistance(orderSnapshot,driverSnapshotArray,orderContext);

        return orderContext;

    }
    public static void initBase(OrderSnapshot orderSnapshot,
                                DriverSnapshot[] driverSnapshotArray,
                                OrderContext orderContext){
        orderContext.orderNo = orderSnapshot.getOrderId();
        orderContext.time = orderSnapshot.getDateTime();
        orderContext.lat = Double.valueOf(orderSnapshot.getLat());
        orderContext.lon = Double.valueOf(orderSnapshot.getLon());
        orderContext.driverSnapshotArray = driverSnapshotArray;
    }
    public static void initDistance(OrderSnapshot orderSnapshot,
                                    DriverSnapshot[] driverSnapshotArray,
                                    OrderContext orderContext){
        ArrayList<TreeMap<Double,DriverSnapshot>> filterDriverSnapshot = new ArrayList<>();
        DriverSnapshot[] driverSnapshots = driverSnapshotArray;
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

            Double distance = JavaUtil.calculateLineDistance(orderSnapshot.getLat(),orderSnapshot.getLon(),
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


    public static void main(String[] args) {
        String day1 = "0801I";
        String day2 = "0801S";
        String day3 = "0817I";
        String day4 = "0817S";
        String day = "0817";
        String day0801 = "0801";
        int gap = 1;
        //genOneDay(day1,gap);
        //genOneDay(day2,gap);
        //genOneDay(day0801,gap);

        //genOneDay(day4,gap);
       //genOneDay(day,gap);
        //genOneDay(day0801,gap);




    }
}
