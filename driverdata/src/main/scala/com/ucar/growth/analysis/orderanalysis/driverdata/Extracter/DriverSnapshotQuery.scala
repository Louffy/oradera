package com.ucar.growth.analysis.orderanalysis.driverdata.Extracter

import java.util

import com.google.gson.{Gson, JsonObject}
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.{DriverStatus, DriverSnapshot}
import com.ucar.growth.analysis.orderanalysis.driverdata.DispatchContext.OrderSnapshot


/**
  * Created by zfx on 2016/8/11.
*/
object DriverSnapshotQuery extends Serializable{
  val sparkSession = OrderSnapshot.sparkSession
  @transient lazy val gson = new Gson();

  def queryDriverSnapshotArray(cityId:String,driverArray:Array[String],date:String,time:String): Array[DriverSnapshot] ={
    val statusArray = DriverStatusQuery.queryDriverStatus(cityId,driverArray,date,time)
    val positionArray = DriverPositionQuery.queryDriverPosition(cityId,driverArray,date,time)
    val snapshotArray = new Array[DriverSnapshot](statusArray.length)
    for(i <- 0 to driverArray.length -1 ){
      val status:DriverStatus = statusArray(i)
      val position = positionArray(i)
      if(status.dirverId.equals(position.key.split("_")(0)))
        snapshotArray(i) = new DriverSnapshot(driverArray(i),date+" "+time,status,position)
      else
        snapshotArray(i) = null

    }
    snapshotArray

  }
  def queryCityDriverSnapshot(cityId:String,date:String,time:String): JsonObject ={
    val array = queryDriverSnapshotArray(cityId,queryCityDriver(cityId,date),date,time)
    val jsonArray = new util.ArrayList[String]
    array.foreach(iter => jsonArray.add(gson.toJson(iter)))
    var jsonResult = new JsonObject()
    jsonResult.addProperty("time",date)
    jsonResult.addProperty("snapshot",gson.toJson(jsonArray))
    jsonResult.addProperty("cityId",cityId)
    jsonResult

  }
  def queryCityDriver(cityId:String,date:String):Array[String] = {
    val df = sparkSession.read.orc(SourceFile.driverOnlineQueryFile+cityId+"/"+date)
    df.rdd.map(r => r.getAs[String](0)).collect()
  }

  def main(args:Array[String]): Unit ={
    queryCityDriverSnapshot("1","2016-07-10","14:28:13")
    sparkSession.stop()

  }

}
