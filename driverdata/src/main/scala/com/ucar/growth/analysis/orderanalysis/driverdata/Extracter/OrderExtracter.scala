package com.ucar.growth.analysis.orderanalysis.driverdata.Extracter

import com.ucar.growth.analysis.orderanalysis.driverdata.DispatchContext.OrderSnapshot

/**
  * Created by zfx on 2016/8/4.
  */
object OrderExtracter {



  val sparkSession = OrderSnapshot.sparkSession
  def extractOrderData(cityId:String,status:String,service_type:String,dispatch_status:String,invalid_type:String,date:String): Unit = {
    val orderDf = sparkSession.read.format("orc").load(SourceFile.orderFile+date)

    val specialOrder = orderDf.filter(s"start_city_id = $cityId " +
      s"and status = $status " +
      s"and service_type_id = $service_type  " +
      s"and dispatch_status = $dispatch_status " +
      s"and invalid_type = $invalid_type")

    specialOrder.repartition(1).write.mode("overwrite").json(SourceFile.invalidOrderFile+cityId+"/"+date)


  }
  def extractOrderField(cityId:String,date:String): Unit ={
    val df = sparkSession.read.json(SourceFile.invalidOrderFile+cityId+"/"+date)
    val dfsave = df.select("id","order_no","member_id","create_time",
      "estimate_board_time","estimate_board_lon","estimate_board_lat",
      "estimate_off_lon","estimate_off_lat","estimate_board_position",
      "estimate_board_position_detail","estimate_distance","estimate_money","estimate_car_type")
    dfsave.repartition(1).write.mode("overwrite").json(SourceFile.invalidOrderFileField+cityId+"/"+date)
  }
  def main(args: Array[String]) {
    extractOrderData("1","-1","14","4","27","2016-07-20")
    extractOrderField("1","2016-07-20")
    sparkSession.stop()
  }

  /*def actionByDriver(): Unit ={
    val driverActionDf = sparkSession.read.json(DataFiles.driverActionBJFile)
    //val keyDf = driverActionDf.map(Row=>{"driver_action_key":driverActionDf("driver_id"))
    val key = driverActionDf.rdd.keyBy(row => row.fieldIndex("driver_id")).groupByKey()
    val k = driverActionDf.groupBy("driver_id")

    val values  = key.mapValues(iter => iter.toList.sortBy(row=>row.fieldIndex("action_time")))

    values.saveAsTextFile(DataFiles.driverActionBJOrderFile)
  }*/

}
