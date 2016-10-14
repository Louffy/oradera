package com.ucar.growth.analysis.orderanalysis.driverdata.driverdata.Extracter

import java.text.SimpleDateFormat


import com.ucar.growth.analysis.orderanalysis.driverdata.model.{OrderSnapshot, DriverSnapshot}
import com.ucar.growth.analysis.orderanalysis.driverdata.util.{AppConfig, JavaUtil}
import com.ucar.growth.analysis.orderanalysis.model.DriverSnapshot
import JavaUtil
import org.apache.commons.lang3.StringUtils
import org.apache.spark
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SQLContext, SparkSession}

import scala.collection.Map
import scala.collection.Set
import scala.collection.mutable.ArrayBuffer


/**
  * Created by zfx on 2016/7/29.
  */
object DataExtracter extends Serializable{

  val appConfig = AppConfig.getInstance()

  val sparkSession:SparkSession = SparkSession.builder()
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .getOrCreate()

  import sparkSession.implicits._

  val toDouble = sparkSession.udf.register("udf",(field:String)=>field.toDouble)
  val udf_driver_id = sparkSession.udf.register("udf1",
    (arg:String)=>{
      arg match {

        case null => 0
        case s => {
          if (s.length > 10)
            StringUtils.stripStart(StringUtils.stripStart(s.substring(0, 10), "1"), "0").toInt
          else
            0
        }
      }
    })
  var udf_timestamp = sparkSession.udf.register("udf2", func = (date: String) => {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val datetime = dateFormat.parse(date)
    datetime.getTime()
  })
  val udf_key = sparkSession.udf.register("udf3", func = (id:Long,time:Long) => {
    id.toString + "_" + time.toString
  })
  val udf_key_date = sparkSession.udf.register("udf3", func = (id:Long,time:Long) => {
    //val dateString = JavaUtil.timestampToDate(time.toString)
    val timeNew:Long = time/(60000)
    id.toString + "_" + timeNew.toString
  })
  val udf_stamp_minute = sparkSession.udf.register("udf3", func = (time:Int) => {
    //val dateString = JavaUtil.timestampToDate(time.toString)
    val timeNew:Long = time/(60000)
    timeNew
  })
  def initData(orderNo:String): Unit ={
      sparkSession.conf.set("appName",appConfig.get("appName")+"_"+orderNo)
  }

  /**
    * get Order Info
    *
    * @param orderNo
    * @param orderDate
    */
  def extractOrder(orderNo:String,orderDate:String): OrderSnapshot ={
    var date = "*"
    if(orderDate != null)
      date = orderDate
    val orderRow = sparkSession.read.format("orc")
      .load(appConfig.get("hdfsHivePrefix")+appConfig.get("Order")+date)
      .filter(s"order_no = $orderNo").first()

    new OrderSnapshot(orderRow.getAs[String]("order_no"),
      orderRow.getTimestamp(orderRow.fieldIndex("create_time")).toString,
      orderRow.getAs[Int]("start_city_id"),
      orderRow.getDecimal(orderRow.fieldIndex("estimate_board_lat")).toString,
      orderRow.getDecimal(orderRow.fieldIndex("estimate_board_lon")).toString)

  }

  /**
    * extract all drivers by cityId
    *
    * @param order
    */
  def extractDriver(order:OrderSnapshot): Set[Int] ={
    val serviceCityId = order.getCityId()
    val driverSet = sparkSession.read.orc(appConfig.get("hdfsHivePrefix")+appConfig.get("Driver"))
      .filter(s"service_city_id=$serviceCityId")
      .map(row => row.getAs[Int]("id"))
      .collect()
      .toSet[Int]
    driverSet
  }

  /**
    * extract action by all drivers in given city and time(yyyy-mm-dd)
    *
    * @param order
    * @param driverSet
    * @return
    */
  def extractDriverAction(order:OrderSnapshot,driverSet:Set[Int]): Map[Int,Iterable[Row]] ={
    val time = order.getDateTime()
    val date = time.split(" ")(0)
    val driverActionDf = sparkSession.read.json(appConfig.get("hdfsHBasePrefix")
      +appConfig.get("DriverAction")
      +date)
      .select("rowkey","pd_oper_type","td_lat","td_lon","pd_oper_time","pd_remark")
    val newDriverActiondf = driverActionDf.withColumn("driver_id",udf_driver_id(driverActionDf("rowkey")))
    val newDriverAction = newDriverActiondf.na.fill(0,Seq("driver_id"))


    val hashSet = driverSet

    val br = sparkSession.sparkContext.broadcast(hashSet)
    val schema = newDriverAction.schema
    //map-side join
    //logger.error("ZFX:map")
    val driverAct = newDriverAction.rdd.mapPartitions {
      iter => {
        val array = ArrayBuffer[Row]()
        if(iter.isEmpty) {
          //logger.error("ZFX:iter empty")
        }
        else {
          val map = br.value
          iter.foreach { r => {
            if (!r.anyNull) {
              if (map.contains(r.getAs[Int](r.fieldIndex(("driver_id")))))
                array.+=(r)
            }
          }
          }
        }
        array.iterator
      }

    }
    val driverAction = sparkSession.createDataFrame(driverAct,schema)

    //val driverActionBJ = driverAction.join(driverBJDf,driverBJDf.col("id").equalTo(newDriverAction.col("driver_id")),"inner").drop(driverBJDf("id"))
    val driverActionBJTime = driverAction.withColumn("action_time",udf_timestamp(driverAction("pd_oper_time")))
    val driverActionGroup = driverActionBJTime
        .select("driver_id","action_time","pd_oper_type","td_lat","td_lon","pd_remark")
        .rdd
        .keyBy[Int](row => row.getAs[Int]("driver_id"))
        .groupByKey()
        .collectAsMap()

    driverActionGroup

  }

  /**
    * extract driver position
    *
    * @param order
    * @param driverSet
    * @return
    */
  def extractDriverPositionOneMinute(order: OrderSnapshot, driverSet:Set[Int]): Map[String,Row] ={

    val time = order.getDateTime()
    val dateStrings = time.split(" ")(0).split("-")
    val prefix = dateStrings(0)+dateStrings(1)

    val driverPositiondf = sparkSession.read.json(appConfig.get("hdfsHBasePrefix")
      +appConfig.get("DriverPosition")
      +prefix
      +"/"
      +time.split(" ")(0))
      .select("rowkey","pd_lat","pd_lon","oth6_positionTimeLong")


    val driverPositionId = driverPositiondf.withColumn("driver_id",udf_driver_id(driverPositiondf("rowkey")))
    val driverPositionKey = driverPositionId.withColumn("key",udf_key_date(driverPositionId("driver_id"),driverPositionId("oth6_positionTimeLong")))

    val currentTime = JavaUtil.stringToTimestampms(order.getDateTime())/(60*1000)
    val currentTime2 = currentTime -1
    val driverPositionTmp = driverPositionKey
      .select("driver_id","key","pd_lat","pd_lon")
      .filter(row=>{
        if(row.getAs[String]("key").equals(row.getAs[Int]("driver_id").toString+currentTime.toString)
          || row.getAs[String]("key").equals(row.getAs[Int]("driver_id").toString+currentTime2.toString))
          true
        else
          false
      })

    val hashSet = driverSet
    val br = sparkSession.sparkContext.broadcast(hashSet)
    //val driverPosition = newDriverPosition.join(br.value,br.value.col("driver_id").equalTo(newDriverPosition.col("driver_id")),"inner").drop(br.value("driver_id"))


    val driverPos = driverPositionTmp.rdd.mapPartitions[Row](iter => {
      val map = br.value
      val array = ArrayBuffer[Row]()
      iter.foreach{r => {
        if(map.contains(r.getAs[Int](0))){
          array.+= (r)
        }
      }}
      array.iterator
    })

    val driverPositionMap = driverPos.map(r => (r.getAs[String](r.fieldIndex("key")), r))
      .reduceByKey((r1,r2) => r1)
      .collectAsMap()

    driverPositionMap

  }

  /**
    *
    * @param driverSnapshotArray
    * @param file
    */
  def saveDriverSnapshotToJson(driverSnapshotArray:Array[DriverSnapshot],file:String):Unit = {
    val jArray = JavaUtil.ObjectArrayToJson(driverSnapshotArray)
    deleteHDFSFile(file)
    sparkSession.sparkContext.parallelize(jArray,1).saveAsTextFile(file)

  }

  /**
    *
    * @param order
    * @param file
    */
  def saveOrderSnapshotToJson(order:OrderSnapshot,file:String):Unit={
    val oArray = new Array[OrderSnapshot](1)
    oArray(0) = order
    deleteHDFSFile(file)
    sparkSession.sparkContext.parallelize(JavaUtil.ObjectArrayToJson(oArray),1).saveAsTextFile(file)
  }
  def deleteHDFSFile(file:String):Unit={
    val hadoopConf = new org.apache.hadoop.conf.Configuration()
    val hdfs = org.apache.hadoop.fs.FileSystem.get(new java.net.URI(appConfig.get("hdfs")), hadoopConf)
    val path = new org.apache.hadoop.fs.Path(file)
    if (hdfs.exists(path))
        hdfs.delete(path, true)
  }

  /**
    * init data
    *
    * @param orderNo
    * @param date
    */
  def init(orderNo:String,date:String): Unit ={

    initData(orderNo)
    val orderSnapshot = extractOrder(orderNo,date)
    var driverSet = extractDriver(orderSnapshot)
    val driverActionMap = extractDriverAction(orderSnapshot,driverSet)
    driverSet = driverActionMap.keySet
    val driverPositionMap = extractDriverPositionOneMinute(orderSnapshot,driverSet)

    val driverSnapshotArray = CreateDriverSnapshot.createDriverSnapshot(
      orderSnapshot,driverSet,driverActionMap,driverPositionMap)

    saveDriverSnapshotToJson(driverSnapshotArray,appConfig.get("hdfsSavePrefix")
      + appConfig.get("SaveDriverSnapshot")
      + orderNo)
    saveOrderSnapshotToJson(orderSnapshot,appConfig.get("hdfsSavePrefix")
      + appConfig.get("SaveOrderSnapshot")
      + orderNo)

    sparkSession.stop()
  }

  def main(args:Array[String]):Unit={
    init("27315631550415","2016-08-17")
  }

}
