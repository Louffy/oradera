package com.ucar.growth.analysis.orderanalysis.driverdata.driverdata.Extracter

import com.ucar.growth.analysis.orderanalysis.driverdata.model._
import com.ucar.growth.analysis.orderanalysis.driverdata.util.JavaUtil
import com.ucar.growth.analysis.orderanalysis.model._
import com.ucar.growth.analysis.orderanalysis.util
import org.apache.spark.sql.{Row, SQLContext, SparkSession}
import JavaUtil
import scala.collection.Map
import scala.collection.Set
import scala.collection.mutable.ArrayBuffer

/**
  * Created by zfx on 2016/10/12.
  */
object CreateDriverSnapshot {



  def createDriverSnapshot(order:OrderSnapshot,
                           driverSet:Set[Int],
                           driverActionMap:Map[Int,Iterable[Row]],
                           driverPositionMap:Map[String,Row]): Array[DriverSnapshot] = {
    val time = JavaUtil.stringToTimestampms(order.getDateTime())
    val driverSnapshotArray = new Array[DriverSnapshot](driverSet.size)
    var index = 0
    for (id <- driverSet) {
      val ds: DriverStatus = createDriverStatus(id,time,driverActionMap)
      val dp: DriverPosition = createDriverPosition(id,time,driverPositionMap)
      val driverSnapshot = new DriverSnapshot(id,time,ds,dp)
      driverSnapshotArray(index) = driverSnapshot
      index = index + 1
    }

    driverSnapshotArray
  }

  def createDriverPosition(id:Int,time:Long,driverPositionMap:Map[String,Row]):DriverPosition = {
    val t1 = (time/60000)
    val t2 = t1-1

    var r1:Row = null
    if(driverPositionMap.contains(id.toString + "_" + t1.toString))
      r1 = driverPositionMap.get(id.toString + "_" + t1.toString).get

    var r2:Row = null
    if(driverPositionMap.contains(id.toString + "_" + t2.toString))
      r2 = driverPositionMap.get(id.toString + "_" + t2.toString).get

    if(r1 != null)
      new DriverPosition(id,time,r1.getAs[String]("pd_lat"),
        r1.getAs[String]("pd_lon"))
    else if(r2 != null)
      new DriverPosition(id,time,r2.getAs[String]("pd_lat"),
        r2.getAs[String]("pd_lon"))
    else
      return null
  }

  def createDriverStatus(id:Int,time:Long, driverActionMap:Map[Int,Iterable[Row]]):DriverStatus = {
    val iter = driverActionMap.get(id).getOrElse(null)
    var preDriverAction: DriverAction = null
    var folDriverAction:DriverAction = null
    for(i : Row <- iter){

      val t = i.getAs[Long]("action_time")


      if(preDriverAction == null || t <= time && t >= preDriverAction.getAction_time.toLong)
        preDriverAction = new DriverAction(
          i.getAs[Int]("driver_id"),
          i.getAs[Long]("action_time"),
          i.getAs[String]("pd_oper_type"),
          i.getAs[String]("td_lat"),
          i.getAs[String]("td_lon"),
          i.getAs[String]("pd_remark")
        )

      if(folDriverAction == null || t >= time && t <= folDriverAction.getAction_time.toLong)
        folDriverAction = new DriverAction(
          i.getAs[Int]("driver_id"),
          i.getAs[Long]("action_time"),
          i.getAs[String]("pd_oper_type"),
          i.getAs[String]("td_lat"),
          i.getAs[String]("td_lon"),
          i.getAs[String]("pd_remark")
        )
    }

    val driverActionArray = new Array[DriverAction](2)
    driverActionArray(0) = preDriverAction
    driverActionArray(1) = folDriverAction

    constructDriverStatus(id,time,driverActionArray)
  }



  private def constructDriverStatus(driverId: Int, dateTime: Long,
                                    driverActionArray: Array[DriverAction]): DriverStatus = {
    val status: String = checkDriverStatus(driverActionArray, dateTime)
    var availiable: Int = 0

    if (status == "空闲") availiable = 1
    else if (status == "下线") availiable = 2
    else if (status == "服务中-取消") availiable = 3
    else if (status == "小休") availiable = 4
    else if (status == "服务中-预约") availiable = 5

    new DriverStatus(driverId, dateTime, driverActionArray, status, String.valueOf(availiable))
  }

  def checkDriverStatus(driverAction: Array[DriverAction], timeStamp: Long): String = {
    if (driverAction.length < 2)
      return "下线"

    val a1: DriverAction = driverAction(0)
    val a2: DriverAction = driverAction(1)

    if (a1 == null || a2 == null)
      return "下线"
    if (a1.getDriver_id() != a2.getDriver_id())
      return "下线"

    if (a1.getAction_time() == a2.getAction_time())
      return "下线"

    val s1: String = a1.getPd_oper_type()
    val s2: String = a2.getPd_oper_type()
    val t1: Long = a1.getAction_time()
    val t2: Long = a2.getAction_time()

    val timeOrder: Long = timeStamp

    if (s2 == "接单") {
      if ((s1 == "接单") || (s1 == "出发") || (s1 == "到达"))
        return "服务中-取消"
      else if (s1 == "开始服务")
        return "服务中-开始服务"
      else if (t2 - timeOrder < 120 * 1000)
        return "服务中"
      else return "空闲"
    }
    else if (s1 == "更新费用") {
      if ((timeStamp) - t1 < 30 * 1000)
        return "服务中-更新"
    }
    else if (s1 == "服务结束") {
      if (((timeStamp) - t1) > 5 * 60 * 1000)
        return "空闲"
    }
    else if ((s1 == "更新费用") || (s1 == "开启自动接橙单") || (s1 == "开启自动接蓝单") || (s1 == "开启接蓝单")) {
      if (s2 == "出发")
        return "服务中-预约"
      else return "空闲"
    }
    else if (s1 == "上班-正常上班") {
      if ((s2 == "出发") || (s2 == "到达") || (s2 == "开始服务") || (s2 == "服务结束") || (s2 == "更新费用"))
        return "服务中-重新登录-" + s1 + "-" + s2
      else if (t2 - timeOrder < 120 * 1000)
        return "服务中"
      else if (s2 == "登录")
        return "服务中"
      else if (timeOrder - t1 < 30 * 1000)
        return "休息"
      else return "空闲"
    }
    else if ((s1 == "退出") && (s2 == "登录"))
      return "下线"
    else if ((s1 == "下班-临时小休") && (s2 == "上班-正常上班"))
      return "小休"
    else
      return "服务中-" + s1
    return "服务中-" + s1
  }
}