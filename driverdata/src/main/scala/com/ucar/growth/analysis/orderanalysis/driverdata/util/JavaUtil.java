package com.ucar.growth.analysis.orderanalysis.driverdata.util;

import java.text.ParseException;
import java.util.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverStatus;
import com.ucar.growth.analysis.orderanalysis.driverdata.Extracter.DataExtracter;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverStatus;
import com.ucar.growth.analysis.orderanalysis.driverdata.Extracter.DataExtracter;
import com.ucar.growth.analysis.orderanalysis.driverdata.Extracter.SourceFile;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.rdd.RDD;
import org.joda.time.DateTime;


/**
 * Created by zfx on 16/9/5.
 */
public class JavaUtil {

    public static Gson gson = new Gson();

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

    public static <T> String[] ObjectToJson(T[] obj){

        String[] jsonArray = new String[obj.length];

        for(int i = 0;i <  obj.length;i++){
            String json = gson.toJson(obj[i]);
            jsonArray[i] = json;
        }
        return jsonArray;
    }

    public static <T> String[][] ObjectArrayToJson(T[][] obj){

        String[][] jsonArray = new String[obj.length][];

        for(int i = 0;i<obj.length;i++){
            String[] json = ObjectToJson(obj[i]);
            jsonArray[i] = json;
        }
        return jsonArray;
    }
    public static void main(String[] args){
        SparkConf conf = new SparkConf().setAppName("test");
        JavaSparkContext sc = new JavaSparkContext(conf);
        JavaRDD<String> rddseg = sc.textFile("hdfs://namenode01.bi.10101111.com:8020/ml/mlss/citymap/seg2.csv");

    }




}

