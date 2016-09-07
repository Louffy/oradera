package com.ucar.growth.analysis.orderanalysis.driverdata.util;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverStatus;
import com.ucar.growth.analysis.orderanalysis.driverdata.Extracter.DataExtracter;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverStatus;
import com.ucar.growth.analysis.orderanalysis.driverdata.Extracter.DataExtracter;
import org.apache.hadoop.fs.Path;
import org.apache.spark.rdd.RDD;
import org.apache.spark.sql.DataFrame;
import org.joda.time.DateTime;


/**
 * Created by zfx on 16/9/5.
 */
public class Util {

    @transient  Gson gson = new Gson();

    public static long stringToTimestamp(String date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date datetime = dateFormat.parse(date);
        return datetime.getTime();
    }
    public static long stringToTimestampms(String date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date datetime = dateFormat.parse(date);
        return datetime.getTime();
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
    def ObjectArrayToJson[T](obj:Array[Array[T]]) : Array[Array[String]] = {

                val jsonArray = new Array[Array[String]](obj.length)

        for( i <- 0 to  obj.length-1){
            val json = ObjectToJson(obj(i))
            jsonArray(i) = json
        }
        jsonArray
        }
        def saveHdfsFile[T](rdd:RDD[T],file:String): Unit ={

        if(SourceFile.hdfs.exists(new Path(file))){
            SourceFile.hdfs.delete(new Path(file),true)
        }

        rdd.saveAsTextFile(file)
        }
        def calculateLineDistance(lats: String,lons:String,late:String,lone:String):Double = {

                val R = 12742001.579854401:Double
        val P = 0.01745329251994329:Double
        val lon1 = P * lons.toDouble
        val lat1 = P * lats.toDouble
        val lon2 = P * lone.toDouble
        val lat2 = P * late.toDouble

        val d1 = Math.sin(lon1)
        val d2 = Math.sin(lat1)
        val d3 = Math.cos(lon1)
        val d4 = Math.cos(lat1)
        val d5 = Math.sin(lon2)
        val d6 = Math.sin(lat2)
        val d7 = Math.cos(lon2)
        val d8 = Math.cos(lat2)
        val tmpresult = Math.sqrt((d4 * d3 - d8 * d7) * (d4 * d3 - d8 * d7) + (d4 * d1 - d8 * d5) * (d4 * d1 - d8 * d5) + (d2 - d6) * (d2 - d6));
        val result = Math.asin(tmpresult/ 2.0) * R
        result
        }



    }

