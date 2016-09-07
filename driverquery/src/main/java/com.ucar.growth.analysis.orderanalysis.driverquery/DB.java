package com.ucar.growth.analysis.orderanalysis.driverquery;

import java.io.*;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverAction;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverPosition;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.OrderSnapshot;
import redis.clients.jedis.Jedis;

/**
 * Created by zfx on 2016/8/17.
 */
// position: hash : minutes,driverid,value:lat_lon
// action: hash: driverid,timestamp,value (range search)
public class DB {
    private static DB ourInstance = new DB();
    //public static final String SSDB_IP = "10.104.101.63";
    public static final String SSDB_IP = "localhost";
    //public static final int SSDB_PORT = 8888;
    public static final int SSDB_PORT = 6379;

    public static Jedis dbJedis;


    public static DB getInstance() {
        return ourInstance;
    }

    private DB() {
        dbJedis = new Jedis(SSDB_IP,SSDB_PORT);
    }
    public void importDriverPosition(String folder){
        Gson gson = new Gson();
        Jedis jedis = new Jedis(SSDB_IP,SSDB_PORT);
        File listFiles = new File(folder);
        if(listFiles!=null) {
            File[] listF = listFiles.listFiles();
            for (File file : listF) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = br.readLine()) != null) {
                        DriverPosition driverPosition = gson.fromJson(line, DriverPosition.class);
                        String[] tmp = driverPosition.key().split("_");
                        if (tmp.length >= 2) {
                            jedis.hset(tmp[1], tmp[0], driverPosition.pd_lat() + "_" + driverPosition.pd_lon());
                            System.out.println(driverPosition.key());

                        }
                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        jedis.close();
    }
    public void importDriverAction(String folder){
        Gson gson = new Gson();
        Jedis jedis = new Jedis(SSDB_IP,SSDB_PORT);
        File listFiles = new File(folder);
        if(listFiles!=null) {
            File[] listF = listFiles.listFiles();
            for (File file : listF) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = br.readLine()) != null) {
                        DriverAction driverAction = gson.fromJson(line, DriverAction.class);
                        if(driverAction == null)
                            continue;
                        if(driverAction.getKey() == null)
                            continue;
                        String[] tmp = driverAction.getKey().split("_");
                        if (tmp.length >= 2) {
                            jedis.hset(tmp[0], tmp[1], driverAction.getPd_oper_type() + "_" + driverAction.getTd_lat() + "_" + driverAction.getTd_lon()+"_"+driverAction.getPd_remark());
                            System.out.println(driverAction.key());
                        }
                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        jedis.close();
    }
    public void importInvalidOrder(String folder){
        File listFiles = new File(folder);
        if(listFiles!=null) {
            File[] listF = listFiles.listFiles();
            for (File file : listF) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = br.readLine()) != null) {
                        JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject();
                        if(jsonObject == null)
                            continue;
                        dbJedis.set("o"+jsonObject.get("order_no").getAsString(),
                                jsonObject.get("create_time").getAsString()+"_"
                        +jsonObject.get("estimate_board_lat").getAsString()+"_"+
                                        jsonObject.get("estimate_board_lon").getAsString());

                        System.out.println(jsonObject.get("order_no").getAsString());
                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    public DriverAction[] getActionList(String driverId){
        TreeMap<String,String> acTree = new TreeMap<String,String>(dbJedis.hgetAll(driverId));
        ArrayList<DriverAction> driverActions = new ArrayList<>();
        Iterator<String> it = acTree.keySet().iterator();
        while(it.hasNext()){
            String time = it.next();
            String value = acTree.get(time);
            String[] strings = value.split("_");
            DriverAction driverAction = new DriverAction(time,driverId,strings[0],strings[1],strings[2],strings[3]);
            driverActions.add(driverAction);
        }
        DriverAction[] driverActions1 = new DriverAction[driverActions.size()];
        return driverActions.toArray(driverActions1);

    }
    public DriverPosition getLocation(String driverId,String timeStamp){

        String minutes = String.valueOf(Long.valueOf(timeStamp)/60000);
        String key = driverId+"_"+minutes;

        String loc = dbJedis.hget(minutes,driverId);
        DriverPosition driverPosition = new DriverPosition(key,loc.split("_")[0],loc.split("_")[1]);
        return driverPosition;
    }
    public HashMap<String,DriverPosition> getLocationList(String timeStamp){
        HashMap<String,DriverPosition> driverPositionHashMap = new HashMap<>();
        System.out.println(timeStamp);
        String minutes = String.valueOf(Long.valueOf(timeStamp)/60000);
        Map<String,String> locList = dbJedis.hgetAll(minutes);
        Iterator<String> it = locList.keySet().iterator();
        while(it.hasNext()){
            String driverId = it.next();
            String loc = locList.get(driverId);
            String key = driverId+"_"+minutes;
            DriverPosition driverPosition = new DriverPosition(key,loc.split("_")[0],loc.split("_")[1]);
            driverPositionHashMap.put(driverId,driverPosition);
        }
        return driverPositionHashMap;
    }

    public DriverAction[] getAction(String driverId,String timeStamp){
        Map<String,String> actionMap = dbJedis.hgetAll(driverId);
        Iterator<String> it = actionMap.keySet().iterator();
        String low = "0";
        String up = "9999999999";
        while(it.hasNext()){
            String current = it.next();
            if((current.compareTo(low)>0)&&(timeStamp.compareTo(current)>0))
                low = current;
            if((current.compareTo(up)<0)&&(timeStamp.compareTo(current)<0))
                up = current;
        }
        DriverAction[] driverActions = new DriverAction[2];

        if(actionMap.containsKey(low)) {
            String[] strings = actionMap.get(low).split("_");
            if (strings.length < 4)
                driverActions[0] = null;
            else
                driverActions[0] = new DriverAction(low, driverId,
                    strings[0],strings[1], strings[2], strings[3]);
        }
        if(actionMap.containsKey(up)) {
            String[] strings = actionMap.get(up).split("_");
            if (strings.length < 4)
                driverActions[1] = null;
            else
                driverActions[1] = new DriverAction(up, driverId,
                    strings[0],strings[1], strings[2], strings[3]);
        }
        return driverActions;
    }

    public ArrayList<DriverAction> getAdvanceActions(String driverId,String timeStamp){
        Map<String,String> actionMap = dbJedis.hgetAll(driverId);
        TreeMap<String,String> actionTree = new TreeMap<>(actionMap);
        Iterator<String> it = actionMap.keySet().iterator();
        ArrayList<DriverAction> temp = new ArrayList<>();
        String key = timeStamp;
        while(actionTree.higherKey(key) != null){
            String time = actionTree.higherKey(key);
            String[] strings = actionMap.get(time).split("_");
            if(strings[0].equals("接单"))
                temp.add(new DriverAction(time,driverId,strings[0],strings[1], strings[2], strings[3]));
            if(temp.size() == 1&&strings[0].equals("到达"))
                temp.add(new DriverAction(time,driverId,strings[0],strings[1], strings[2], strings[3]));
            if(temp.size() == 2)
                break;
            key = time;
        }
        if(temp.size()==2)
            return temp;
        else
            return null;
    }
    public Map<String,String> getCityDriverList(String timeStamp){
        String minutes = String.valueOf(Long.valueOf(timeStamp)/60000);
        return dbJedis.hgetAll(minutes);
    }
    public OrderSnapshot getInvalidOrder(String orderNo){
        String s = dbJedis.get("o"+orderNo);
        return new OrderSnapshot(orderNo,s.split("_")[0],s.split("_")[1],s.split("_")[2]);
    }
    public OrderSnapshot getInvalidOrder2(String orderNo){
        String s = dbJedis.get(orderNo);
        if(s == null || s.split("_").length<3)
            return null;
        System.out.println(s.split("_")[0]);
        return new OrderSnapshot(orderNo,s.split("_")[0],s.split("_")[1],s.split("_")[2]);
    }
    public String[] getInvalidOrderList(){
        Set<String> keys = dbJedis.keys("o2*");
        String[] orders = new String[keys.size()];
        return keys.toArray(orders);
    }

    public void show(){
        HashMap<String,String> driverlist = (HashMap<String,String>)DB.getInstance().getCityDriverList("1468980399000");
        Iterator<String> drivers = driverlist.keySet().iterator();
        int count = 0;
        HashSet<String> d = new HashSet<String>();
        ArrayList<Long> time = new ArrayList<>();
        long sum = 0;
        while(drivers.hasNext()){
            String id = drivers.next();
            HashMap<String,String> temp = (HashMap<String,String>)dbJedis.hgetAll(id);
            HashMap<String,String> t2 = new HashMap<>();
            Iterator<String> it = temp.keySet().iterator();
            //while(it.hasNext()){
            //     String t = it.next();
            //     String date = JavaUtil.timestampToDateString(t);
            //     t2.put(date,temp.get(t));
            //System.out.println(t+" "+date);
            //  }

            TreeMap<String,String> treeMap = new TreeMap<>(temp);
            Iterator<Map.Entry<String,String>> iterator = treeMap.entrySet().iterator();
            int flag = 0;
            String pre = "0";
            while(iterator.hasNext()){
                Map.Entry<String,String> entry = iterator.next();
                String value = entry.getValue().split("_")[0];
                if(id.equals("17553")){
                    System.out.println(entry.getKey()+" " + entry.getValue());

                }
                //System.out.println(id+":"+value);
                if(value.equals("上班-正常上班"))
                    flag = 1;
                else if(value.equals("服务结束")&&flag==1) {
                    count++;
                    flag = 0;
                    d.add(id);
                    long dis = (Long.valueOf((entry.getKey()))-Long.valueOf((pre)))/(60*1000);
                    System.out.println(id + ":" + dis + " " + pre + " " +entry.getKey());
                    time.add(dis);
                    sum += dis;

                }
                else
                    flag =0;
                //System.out.println(flag);
                pre = entry.getKey();

            }

        }
        System.out.println(driverlist.size());
        System.out.println(d.size());
        System.out.println(count);
        System.out.println(sum/count);
    }
    public static void main(String[] args){
        DB db = DB.getInstance();
       // System.out.println(args[0]);
       // db.importDriverPosition(args[0]);
        //System.out.printf(args[1]);
        //db.importDriverAction("data/nac");
        //db.importInvalidOrder("data/order");
        Map<String,DriverPosition> d = db.getLocationList("1469011368934");
        System.out.println(d.size());

    }

}
