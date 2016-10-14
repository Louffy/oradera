package com.ucar.growth.analysis.orderanalysis.driverdata.util;

import java.text.ParseException;
import java.util.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.google.gson.Gson;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.joda.time.DateTime;


/**
 * Created by zfx on 16/9/5.
 */
public class JavaUtil {

    public static Gson gson = new Gson();

    /**
     * yyyy-MM-dd HH:mm:ss to Long
     * @param date
     * @return
     */
    public static Long stringToTimestamp(String date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try{
            Date datetime = dateFormat.parse(date);
            return datetime.getTime();
        }catch (ParseException e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * yyyy-MM-dd HH:mm:ss.SSS to Long
     * @param date
     * @return
     */
    public static Long stringToTimestampms(String date){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {
            Date datetime = dateFormat.parse(date);
            return datetime.getTime();
        }catch (ParseException e){
            e.printStackTrace();
        }
        return null;
    }
    public static DateTime timestampToDateTime(long timeStamp){
        return new DateTime(timeStamp);
    }
    public static String timestampToDateString(Timestamp time){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(time);
    }

    public static <T> String[] ObjectArrayToJson(T[] obj){

        String[] jsonArray = new String[obj.length];

        for(int i = 0;i <  obj.length;i++){
            String json = gson.toJson(obj[i]);
            jsonArray[i] = json;
        }
        return jsonArray;
    }

    public static <T> String[][] Object2ArrayToJson(T[][] obj){

        String[][] jsonArray = new String[obj.length][];

        for(int i = 0;i<obj.length;i++){
            String[] json = ObjectArrayToJson(obj[i]);
            jsonArray[i] = json;
        }
        return jsonArray;
    }

    public static Double calculateLineDistance(String lats,String lons,String late,String lone){

        double R = 12742001.579854401;
        double P = 0.01745329251994329;
        double lon1 = P * Double.valueOf(lons);
        double lat1 = P * Double.valueOf(lats);
        double lon2 = P * Double.valueOf(lone);
        double lat2 = P * Double.valueOf(late);

        double d1 = Math.sin(lon1);
        double d2 = Math.sin(lat1);
        double d3 = Math.cos(lon1);
        double d4 = Math.cos(lat1);
        double d5 = Math.sin(lon2);
        double d6 = Math.sin(lat2);
        double d7 = Math.cos(lon2);
        double d8 = Math.cos(lat2);
        double tmpresult = Math.sqrt((d4 * d3 - d8 * d7) *
                (d4 * d3 - d8 * d7) + (d4 * d1 - d8 * d5) *
                (d4 * d1 - d8 * d5) + (d2 - d6) * (d2 - d6));
        double result = Math.asin(tmpresult/ 2.0) * R;
        return result;
    }
    public static void main(String[] args){
        SparkConf conf = new SparkConf().setAppName("test");
        JavaSparkContext sc = new JavaSparkContext(conf);

        JavaRDD<String> rddseg = sc.textFile("hdfs://namenode01.bi.10101111.com:8020/ml/mlss/citymap/seg2.csv");

    }




}

