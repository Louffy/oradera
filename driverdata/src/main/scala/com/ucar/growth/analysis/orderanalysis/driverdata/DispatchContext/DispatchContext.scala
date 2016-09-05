package com.ucar.growth.analysis.orderanalysis.driverdata.DispatchContext


/**
  * Created by zfx on 2016/8/9.
  */
object DispatchContext {
  val sparkSession = OrderSnapshot.sparkSession
  def main(args:Array[String]): Unit ={

  }
  /*def orderContext(cityId:String,date:String,time:String): Unit ={
    val driverDf = sparkSession.read.orc(DataFiles.driverOnlineQueryFile+cityId+"/"+date.split(" ")(0))
    val driverArray = driverDf.select("driver_id").collect().asInstanceOf[Array[String]]
    val timeArray = new Array[String](1)
    timeArray(0) = time
    val driverStatus = DriverStatusQuery.queryDriverStatus(cityId,driverArray,date,timeArray)
    val list:util.ArrayList[DriverStatus] = new util.ArrayList[DriverStatus]
    driverStatus.foreach(iter => {
      list.add(iter(0))
    })
    val df = sparkSession.sparkContext.parallelize(list.toArray())
    Util.saveHdfsFile(df,DataFiles.driverStatus)



    //val driverPosition = DriverPositionQuery.queryDriverPositionJson(cityId,driverArray,date)

  }*/
}
