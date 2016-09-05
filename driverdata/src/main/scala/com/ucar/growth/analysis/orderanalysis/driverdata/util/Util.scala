package com.ucar.growth.analysis.orderanalysis.driverdata.util

import java.sql.{Date, Timestamp}
import java.text.SimpleDateFormat
import java.util.ArrayList

import com.google.gson.Gson
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.{DriverStatus, DriverAction}
import com.ucar.growth.analysis.orderanalysis.driverdata.Extracter.{SourceFile, DataExtracter}
import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import org.joda.time.DateTime

import scala.collection.mutable

/**
  * Created by zfx on 2016/8/4.
  */
object Util extends Serializable{
  @transient lazy val gson:Gson = new Gson()

  def stringToTimestamp(date: String): Long = {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val datetime = dateFormat.parse(date)
    datetime.getTime()
  }
  def stringToTimestampms(date: String): Long = {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    val datetime = dateFormat.parse(date)
    datetime.getTime()
  }
  def timestampToDateTime(timeStamp:Long):DateTime = {
    new DateTime(timeStamp)
  }
  def timestampToDateString(time:Timestamp):String = {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.format(time)
  }

  def countByHour(dataFrame: DataFrame,timeField: String,savePath: String): Unit ={

  }
  def countRows(array: mutable.ArrayBuffer[String], path1:String, path2:String): Unit ={
    var r = new ArrayList[String]()
    array.foreach((p:String) => {
      val df = DataExtracter.sparkSession.read.load(p)
      r.add(df.count().toString)
    })
    //DataExtracter.sparkSession.createData
    //DataExtracter.sparkSession.createDataset[String](r).write.json(path1)
    //DataExtracter.sparkSession.createDataset[String](array).toDF().write.json(path2)

  }
  def ObjectToJson[T](obj:Array[T]) : Array[String] = {

    val jsonArray = new Array[String](obj.length)

     for( i <- 0 to  obj.length-1){
      val json = gson.toJson(obj(i))
       jsonArray(i) = json
    }
    jsonArray
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

  def main(args:Array[String]): Unit ={
    val time = "2016-07-10 16:30:25"
    println(stringToTimestamp(time))
    val a = "195702_046806543400000"
    val b = "95702_0468079991000"
    println(a<=b)
    val array = new Array[DriverAction](2)

    val to = new DriverStatus("23","34",array,"sf","sdf")
    println(gson.toJson(to))
    println(gson.toJson(array))
    println(calculateLineDistance("39.92000","116.46000","45.75000","126.63000"))
    val dt = timestampToDateTime(("1468943937000").toLong)
    println(dt.year().getAsString)
    println(dt.monthOfYear().getAsString)
    println(dt.dayOfMonth().getAsString)
    println(dt.hourOfDay().getAsString)
    println(dt.minuteOfHour().getAsString)

  }

}
