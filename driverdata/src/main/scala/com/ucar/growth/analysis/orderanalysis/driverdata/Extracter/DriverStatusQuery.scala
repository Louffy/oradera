package com.ucar.growth.analysis.orderanalysis.driverdata.Extracter


import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.{DriverStatus, DriverAction}
import com.ucar.growth.analysis.orderanalysis.driverdata.DispatchContext.OrderSnapshot
import com.ucar.growth.analysis.orderanalysis.driverdata.util.Util
import org.apache.log4j.LogManager
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row


object DriverStatusQuery extends Serializable{

  val sparkSpression = OrderSnapshot.sparkSession
  @transient lazy val logger = LogManager.getRootLogger


  implicit val driverActionOrdering = new Ordering[DriverAction] {
    override def compare(x:DriverAction,y:DriverAction): Int ={
      x.key.compareTo(y.key)
    }
  }

  def queryDriverStatus(cityId:String,driverArray:Array[String],date:String,time:String): Array[DriverStatus] = {
    val driverActionDf = sparkSpression.read.json(SourceFile.driverActionQueryFile+cityId+"/"+date)
    val queryResultArray = new Array[DriverStatus](driverArray.length)

    val actionDf:RDD[DriverAction] = driverActionDf.rdd.map{
      r:Row => new DriverAction(r.getAs[String](r.fieldIndex("key")),
      r.getAs[String](r.fieldIndex("driver_id")),
      r.getAs[String](r.fieldIndex("pd_oper_type")),
        r.getAs[String](r.fieldIndex("pd_remark")),
      r.getAs[String](r.fieldIndex("td_lat")),
      r.getAs[String](r.fieldIndex("td_lon")))
    }
    actionDf.cache()
    for(i <- 0 to driverArray.length-1){
        val queryKey = driverArray(i) + "_" + Util.stringToTimestamp(date + " " + time)
        val actionArray = queryDriverActionFromRDD(actionDf, queryKey)
        queryResultArray(i) = constructDriverStatus(driverArray(i), date + " " + time, actionArray)
    }
    queryResultArray

  }
  def queryDriverStatus(cityId:String,driverId:String,date:String,timeArray:Array[String]): Array[DriverStatus] = {
    val driverActionDf = sparkSpression.read.json(SourceFile.driverActionQueryFile+cityId+"/"+date)
    val queryResultArray = new Array[DriverStatus](timeArray.length)

    val actionDf:RDD[DriverAction] = driverActionDf.rdd.map{
      r:Row => new DriverAction(r.getAs[String](r.fieldIndex("key")),
        r.getAs[String](r.fieldIndex("driver_id")),
        r.getAs[String](r.fieldIndex("pd_oper_type")),
        r.getAs[String](r.fieldIndex("pd_remark")),
        r.getAs[String](r.fieldIndex("td_lat")),
        r.getAs[String](r.fieldIndex("td_lon")))
    }
    actionDf.cache()
    for(j <- 0 to timeArray.length-1) {
        val queryKey = driverId + "_" + Util.stringToTimestamp(date + " " + timeArray(j))
        val actionArray = queryDriverActionFromRDD(actionDf, queryKey)
        queryResultArray(j) = constructDriverStatus(driverId, date + " " + timeArray(j), actionArray)
    }
    queryResultArray

  }
  def constructDriverStatus(driverId:String,date:String,driverActionArray: Array[DriverAction]):DriverStatus = {
    val status = checkDriverStatus(driverActionArray)
    var availiable = "0"
    if(status == "空闲")
    availiable = "1"
    new DriverStatus(driverId,date,driverActionArray,checkDriverStatus(driverActionArray),availiable)
  }

  def queryDriverStatus(cityId:String,driverId:String,date:String,time:String):DriverStatus = {
    val driverActionDf = sparkSpression.read.json(SourceFile.driverActionQueryFile+cityId+"/"+date)
    val queryKey = driverId + "_" +Util.stringToTimestamp(date+" " + time)

    val actionDf:RDD[DriverAction] = driverActionDf.rdd.map{
      r:Row => new DriverAction(
        r.getAs[String](r.fieldIndex("key")),
        r.getAs[String](r.fieldIndex("driver_id")),
        r.getAs[String](r.fieldIndex("pd_oper_type")),
        r.getAs[String](r.fieldIndex("pd_remark")),
        r.getAs[String](r.fieldIndex("td_lat")),
        r.getAs[String](r.fieldIndex("td_lon")))
    }
    val actionArray = queryDriverActionFromRDD(actionDf,queryKey)
    constructDriverStatus(driverId,date+" " + time,actionArray)

  }
  // query driver action by id and time from actionDf
  private def queryDriverActionFromRDD(actionDf:RDD[DriverAction],queryKey:String):Array[DriverAction] = {

    val lowerAction = queryLowerDriverActionFromRDD(actionDf,queryKey)
    val upperAction = queryUpperDriverActionFromRDD(actionDf,queryKey)
    val actionArray = new Array[DriverAction](2)
    actionArray(0) = lowerAction
    actionArray(1) = upperAction
    return actionArray

  }
  // find lower_bound driver action
  private def queryLowerDriverActionFromRDD(actionDf:RDD[DriverAction], queryKey:String):DriverAction = {


    val lowerBoundDf = actionDf.filter(r => r.key.compareTo(queryKey)<=0)

    val driverAction = lowerBoundDf.max()

    driverAction


  }
  // find uper_bound driver action
  private def queryUpperDriverActionFromRDD(actionDf:RDD[DriverAction], queryKey:String):DriverAction = {

    val upperBoundDf = actionDf.filter(r => (r.key.compareTo(queryKey))>=0)

    val driverAction = upperBoundDf.min()

    driverAction

  }

  private def checkDriverStatus(driverAction:Array[DriverAction]):String = {

    if(driverAction.length<2)
      return "下线"
    val a1 = driverAction(0)
    val a2 = driverAction(1)
    if(a1 == null || a2 == null)
      return "下线"
    if(a1.driver_id != a2.driver_id)
      return "下线"

    val s1 = a1.pd_oper_type
    val s2 = a2.pd_oper_type

    if(s1 == "登录"||s1 == "上班-正常上班"||s1 == "更新费用"||s1 == "开启接蓝单"||s1 == "开启自动接橙单")
      return "空闲"
    else
      return s1

  }



  def main(args:Array[String]): Unit = {
    val fileArray = new Array[String](1)
    fileArray(0) = SourceFile.driverActionQueryFile
    val cityArray = new Array[String](3)
    for(i <- 0 to 2){
      cityArray(i) = "1"
    }
    val cityId = "1"
    val driverArray = new Array[String](3)
    driverArray(0) = "9587"
    driverArray(1) = "11811"
    driverArray(2) = "56900"

    val timeArray = new Array[String](3)
    val date = "2016-07-10 16:30:25"



    //val jsonArray = JavaUtil.ObjectToJson(resultArray)
   // val rdd = sparkSpression.sparkContext.parallelize(jsonArray)



    sparkSpression.stop()

  }

}
