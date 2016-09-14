package com.ucar.growth.analysis.orderanalysis.driverdata.Extracter

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem

/**
  * Created by zfx on 2016/8/5.
  */
object SourceFile extends Serializable{
  val hdfsPrefix = "hdfs://namenode01.bi.10101111.com:8020/user/hive-0.13.1/warehouse/"
  val hdfsBasePrefix = "hdfs://namenode01.bi.10101111.com:8020/user/hbase/"
  val savePrefix="hdfs://namenode01.bi.10101111.com:8020/user/fx.zhang/OrderDispatchAnalysis/"

  val orderFile = hdfsPrefix + "t_scd_order_all/dt="
  val driverFile = hdfsPrefix + "t_scd_driver"
  val driverActionFile = hdfsBasePrefix + "t_scd_driver_action/"
  val driverPositionFile = hdfsBasePrefix + "t_scd_driver_position_201608/"

  val driverQueryFile = savePrefix + "driverQueryFile/"
  val driverOnlineQueryFile = savePrefix + "driverOnlineQueryFile/"
  val driverActionQueryFile = savePrefix + "driverActionQueryFile/"
  val driverPositionQueryFile = savePrefix + "driverPositionQueryFile/"

  val driverPositionMinuteQueryFile = savePrefix + "driverPositionMinuteQueryFile/"

  val invalidOrderFile = savePrefix + "orderAnalysis/invalidOrder/"
  val successOrderFile = savePrefix + "orderAnalysis/successOrder/"
  val invalidOrderFileField = savePrefix + "orderAnalysis/invalidOrderField/"
  val driverSnapshot = savePrefix + "driverSnapshot/"
  val citydriverSnapshot = savePrefix + "citydriverSnapshot/"




  val driverStatus = savePrefix + "driverStatus/07-10/"

  val startTime = "2016-07-01"
  val endTime = "2016-08-01"
  val countkeyFile = savePrefix + "countkey"
  val countvalueFile = savePrefix + "countvalue"

  val conf:Configuration = new Configuration()
  val hdfs:FileSystem = FileSystem.get(conf)

}
