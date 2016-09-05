package com.ucar.growth.analysis.orderanalysis.driverdata.Extracter

import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverPosition
import com.ucar.growth.analysis.orderanalysis.driverdata.DispatchContext.OrderSnapshot
import com.ucar.growth.analysis.orderanalysis.driverdata.util.Util
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row

/**
  * Created by zfx on 2016/8/10.
  */
object DriverPositionQuery extends Serializable{
  val sparkSpression = OrderSnapshot.sparkSession
  implicit val driverPositionOrdering = new Ordering[DriverPosition] {
    override def compare(x:DriverPosition,y:DriverPosition): Int ={
      x.key.compareTo(y.key)
    }
  }

  //query driver list in one city and one day
  def queryDriverPosition(cityId:String,driverArray:Array[String],date:String,time:String): Array[DriverPosition] = {
    val driverPositionDf = sparkSpression.read.json(SourceFile.driverPositionQueryFile+cityId+"/"+date)
    val queryResultArray = new Array[DriverPosition](driverArray.length)

    val positionDf:RDD[DriverPosition] = driverPositionDf.rdd.map{
      r:Row => new DriverPosition(
        r.getAs[String](r.fieldIndex("key")),
        r.getAs[String](r.fieldIndex("pd_lat")),
        r.getAs[String](r.fieldIndex("pd_lon")))
    }
    positionDf.cache()
    for(i <- 0 to driverArray.length-1){
        val queryKey = driverArray(i) + "_" + Util.stringToTimestamp(date + " " + time)
        val queryResult = queryDriverPositionFromRDD(positionDf, queryKey)
        queryResultArray(i) = queryResult
    }
    queryResultArray
  }
  def queryDriverPosition(cityId:String,driverId:String,date:String,timeArray:Array[String]): Array[DriverPosition] = {
    val driverPositionDf = sparkSpression.read.json(SourceFile.driverPositionQueryFile+cityId+"/"+date)
    val queryResultArray = new Array[DriverPosition](timeArray.length)

    val positionDf:RDD[DriverPosition] = driverPositionDf.rdd.map{
      r:Row => new DriverPosition(r.getAs[String](
        r.fieldIndex("key")),

        r.getAs[String](r.fieldIndex("pd_lat")),
        r.getAs[String](r.fieldIndex("pd_lon")))
    }
    positionDf.cache()
    for(j <- 0 to timeArray.length -1) {
        val queryKey = driverId + "_" + Util.stringToTimestamp(date + " " + timeArray(j))
        val queryResult = queryDriverPositionFromRDD(positionDf, queryKey)
        queryResultArray(j) = queryResult
    }
    queryResultArray
  }

  //query driver position by driverid and time
  def queryDriverPosition(cityId:String,driverId:String,date:String,time:String): DriverPosition ={
    val driverPositionDf = sparkSpression.read.json(SourceFile.driverPositionQueryFile+cityId+"/"+date)

    val positionDf:RDD[DriverPosition] = driverPositionDf.rdd.map{
      r:Row => new DriverPosition(
        r.getAs[String](r.fieldIndex("key")),
        r.getAs[String](r.fieldIndex("pd_lat")),
        r.getAs[String](r.fieldIndex("pd_lon")))
    }

    val queryKey = driverId + "_" + Util.stringToTimestamp(date+" "+time)
    val queryResult =  queryDriverPositionFromRDD(positionDf,queryKey)
    queryResult
  }
  private def queryDriverPositionFromRDD(positionDf:RDD[DriverPosition],queryKey:String):DriverPosition = {
    val lowerBoundDf = positionDf.filter(r => (r.key <= queryKey))
    val driverPosition = lowerBoundDf.max()
    if(driverPosition == null)
      positionDf.first()
    else
      driverPosition
  }

  def main(args:Array[String]): Unit ={

  }
}
