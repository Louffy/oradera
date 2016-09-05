package com.ucar.growth.analysis.orderanalysis.driverdata.Extracter

import java.text.SimpleDateFormat


import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.{Row, SQLContext, SparkSession}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


/**
  * Created by zfx on 2016/7/29.
  */
object DataExtracter extends Serializable{

  val sparkSession = SparkSession.builder.appName("OrderDispatchAnalysis")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .getOrCreate()

  val toDouble = sparkSession.udf.register("udf",(field:String)=>field.toDouble)
  val udf_driver_id = sparkSession.udf.register("udf1",
    (arg:String)=>{
      arg match{

        case null => 0
        case  s => {
          if(s.length >10)
            StringUtils.stripStart(StringUtils.stripStart(s.substring(0,10),"1"),"0").toInt
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
    //val dateString = Util.timestampToDate(time.toString)
    val timeNew:Long = time/(60000)
    id.toString + "_" + timeNew.toString
  })
  val udf_stamp_minute = sparkSession.udf.register("udf3", func = (time:Int) => {
    //val dateString = Util.timestampToDate(time.toString)
    val timeNew:Long = time/(60000)
    timeNew
  })


  def main(args:Array[String]): Unit ={
    //extractDriver("1")
   //extractDriverQueryDataMonth31("1","2016-07")
    //countFileRows()
    extractDriverAction("1","2016-07-21")
    extractDriverAction("1","2016-07-22")
    extractDriverAction("1","2016-07-23")
    extractDriverAction("1","2016-07-24")

    sparkSession.stop()
  }


  def allOrderCount(sqlContext: SQLContext): Unit ={

    var data_df = sqlContext.read.format("orc").load(SourceFile.orderFile)
    var data = data_df.select("order_no","start_city_id","status","dispatch_status","invalid_type")
    var cityCount = data_df.groupBy("start_city_id","status","dispatch_status","invalid_type").count().toDF("start_city_id","status","dispatch_status","invalid_type","order_count")
    cityCount.rdd.coalesce(1).saveAsTextFile(SourceFile.savePrefix+"cityCount")
    //cityGroup.saveAsTextFile(hdfsPrefix + "/user/fx.zhang/cityGroup/")
  }
  // month: 2016-07
  def extractDriverQueryDataMonth31(cityId:String,month:String): Unit ={
    /*for(i <- 8 to 9){
      extractDriverQueryDataDay(cityId,month+"-0"+i.toString)
    }*/
   /* for(i <- 0 to 9){
      extractDriverQueryDataDay(cityId,month+"-1"+i.toString)
    }*/
    for(i <- 0 to 9){
      extractDriverQueryDataDay(cityId,month+"-2"+i.toString)
    }
    /*for(i <- 0 to 1){
      extractDriverQueryDataDay(cityId,month+"-3"+i.toString)
    }*/
  }
  def extractDriverQueryDataDay(cityId:String,date:String): Unit ={

    extractDriverAction(cityId,date)

    extractOnlineDriver(cityId,date)

    extractDriverPositionOneMinute(cityId,date)
  }
  //extract all drivers by cityId
  def extractDriver(serviceCityId:String): Unit ={
    val driverDf = sparkSession.read.orc(SourceFile.driverFile)
    val driverBj = driverDf.filter("service_city_id = "+serviceCityId)


    val driverBJS = driverBj.rdd.map[Int](r => (r.getAs[Int](r.fieldIndex("id"))))
    import sparkSession.implicits._
    driverBJS.toDF("id").write.mode("overwrite").orc(SourceFile.driverQueryFile + serviceCityId)
  }
  //extract active drivers by action_time from action data
  def extractOnlineDriver(cityId:String,time:String): Unit ={
    val driverActionDf = sparkSession.read.json(SourceFile.driverActionQueryFile+cityId+"/"+time)
    val driverGroup = driverActionDf.groupBy("driver_id").count().toDF("driver_id","action_count")
    driverGroup.write.mode("overwrite").orc(SourceFile.driverOnlineQueryFile+cityId + "/"+time)
  }
  //extract action by all drivers in given city and time(yyyy-mm-dd)
  def extractDriverAction(cityId:String,time:String): Unit ={
    val driverActionDf = sparkSession.read.json(SourceFile.driverActionFile+time)
      .select("rowkey","pd_oper_type","td_lat","td_lon","pd_oper_time","pd_remark")
    val newDriverActiondf = driverActionDf.withColumn("driver_id",udf_driver_id(driverActionDf("rowkey")))
    val newDriverAction = newDriverActiondf.na.fill(0,Seq("driver_id"))

    val driverBJDf = sparkSession.read.orc(SourceFile.driverQueryFile+cityId).select("id")
    val hashSet = new mutable.HashSet[Int]()
    driverBJDf.collect().foreach(r=>{
      hashSet.add(r.getAs[Int](0))
    })


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
    val driverActionBJKey = driverActionBJTime.withColumn("key",udf_key(driverActionBJTime("driver_id"),driverActionBJTime("action_time")))
    val driverActionSave = driverActionBJKey.select("key","driver_id","pd_oper_type","td_lat","td_lon","pd_remark")
    driverActionSave.repartition(1).write.mode("overwrite").json(SourceFile.driverActionQueryFile+cityId+"/"+time)


   // var driverActionFormat = driverActionDf.map
  }

  //extract position by active drivers in given city and time
  def extractDriverPosition(cityId:String,time:String): Unit ={
    val driverPositiondf = sparkSession.read.json(SourceFile.driverPositionFile+time)
      .select("rowkey","pd_lat","pd_lon","oth6_positionTimeLong")
    val driverBJDf = sparkSession.read.orc(SourceFile.driverOnlineQueryFile+cityId+"/"+time).select("driver_id")

    val driverPositionId = driverPositiondf.withColumn("driver_id",udf_driver_id(driverPositiondf("rowkey")))
    val driverPositionKey = driverPositionId.withColumn("key",udf_key(driverPositionId("driver_id"),driverPositionId("oth6_positionTimeLong")))
    val driverPositionTmp = driverPositionKey.select("key","driver_id","pd_lat","pd_lon")
    val hashSet = new mutable.HashSet[String]()
    driverBJDf.collect().foreach(r=>{
      hashSet.add(r.getAs[String](0))
    })


    val br = sparkSession.sparkContext.broadcast(hashSet)
    //val driverPosition = newDriverPosition.join(br.value,br.value.col("driver_id").equalTo(newDriverPosition.col("driver_id")),"inner").drop(br.value("driver_id"))

    val schema = driverPositionTmp.schema
    val driverPos = driverPositionTmp.rdd.mapPartitions[Row](iter => {
      val map = br.value
      val array = ArrayBuffer[Row]()
      iter.foreach{r => {
        if(map.contains(r.getAs[String](1))){
          array.+= (r)
        }
      }}
      array.iterator
    })
    val driverPosition = sparkSession.createDataFrame(driverPos,schema)

    val driverPositionSave = driverPosition.select("key","pd_lat","pd_lon")
    driverPositionSave.repartition(30).write.mode("overwrite").json(SourceFile.driverPositionQueryFile+cityId+"/"+time)

  }
  def extractDriverPositionOneMinute(cityId:String,time:String): Unit ={
    val driverPositiondf = sparkSession.read.json(SourceFile.driverPositionFile+time)
      .select("rowkey","pd_lat","pd_lon","oth6_positionTimeLong")
    val driverBJDf = sparkSession.read.orc(SourceFile.driverOnlineQueryFile+cityId+"/"+time).select("driver_id")

    val driverPositionId = driverPositiondf.withColumn("driver_id",udf_driver_id(driverPositiondf("rowkey")))
    val driverPositionKey = driverPositionId.withColumn("key",udf_key_date(driverPositionId("driver_id"),driverPositionId("oth6_positionTimeLong")))
    val driverPositionTmp = driverPositionKey.select("key","driver_id","pd_lat","pd_lon","key")
    val hashSet = new mutable.HashSet[Int]()
    driverBJDf.collect().foreach(r=>{
      hashSet.add(r.getAs[Int](0))
    })


    val br = sparkSession.sparkContext.broadcast(hashSet)
    //val driverPosition = newDriverPosition.join(br.value,br.value.col("driver_id").equalTo(newDriverPosition.col("driver_id")),"inner").drop(br.value("driver_id"))

    val schema = driverPositionTmp.schema
    val driverPos = driverPositionTmp.rdd.mapPartitions[Row](iter => {
      val map = br.value
      val array = ArrayBuffer[Row]()
      iter.foreach{r => {
        if(map.contains(r.getAs[Int](1))){
          array.+= (r)
        }
      }}
      array.iterator
    })
    val driverPosition = driverPos.map(r => (r.getAs[String](r.fieldIndex("key")), r))
      .reduceByKey((r1,r2) => r1)


    val driverPositionSave = sparkSession.createDataFrame(driverPosition.values,schema)

    driverPositionSave.select("key","pd_lat","pd_lon")
          .repartition(1).write.mode("overwrite").json(SourceFile.driverPositionMinuteQueryFile+cityId+"/"+time)

  }


}
