package com.ucar.growth.analysis.orderanalysis.driverdata.DispatchContext

import java.sql.Timestamp

import com.google.gson.JsonObject
import com.ucar.growth.analysis.orderanalysis.driverdata.Extracter.{SourceFile, DriverSnapshotQuery}
import com.ucar.growth.analysis.orderanalysis.driverdata.util.Util

//import org.slf4j.LoggerFactory
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ArrayBuffer

/**
  * Created by feixue zhang
  */
 object OrderSnapshot extends Serializable{

  val sparkSession = SparkSession.builder.appName("OrderDispatchAnalysis")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .getOrCreate()
  //@transient lazy val logger = LoggerFactory.getLogger(getClass.getName)
  def extractInvalidOrderSnapshot(cityId:String,date:String){
    val df = sparkSession.read.orc(SourceFile.invalidOrderFile+cityId+"/"+date)
    val dfinfo = df.select("id","dispatch_time","estimate_board_time","estimate_board_lon",
      "estimate_board_lat")
    val re = new ArrayBuffer[JsonObject]()
    val driverSnapshotQuery = DriverSnapshotQuery
    //logger.error("X:begin foreach")
    val orderCol = dfinfo.collect()
    orderCol.foreach(r => {
      //OrderSnapshot.logger.error("X:iter")
      val time = Util.timestampToDateString(r.getAs[Timestamp](r.fieldIndex("estimate_board_time")))
      val jsonObject = driverSnapshotQuery.queryCityDriverSnapshot(cityId,
        time.split(" ")(0),
        time.split(" ")(1))
      val jsonTmp = new JsonObject()
      jsonTmp.add("citySnap",jsonObject)
      jsonTmp.addProperty("orderId",r.getAs[String](r.fieldIndex("id")))
      jsonTmp.addProperty("lon",r.getAs(r.fieldIndex("estimate_board_lon")).toString())
      jsonTmp.addProperty("lat",r.getAs(r.fieldIndex("estimate_board_lat")).toString())
      jsonTmp.addProperty("estimate_board_time",time)
      re.append(jsonTmp)
    })
    val dfa = sparkSession.sparkContext.parallelize(re).repartition(4)
    Util.saveHdfsFile(dfa,SourceFile.citydriverSnapshot)

  }
   def main(args: Array[String]){
        //println("hello scala")
    extractInvalidOrderSnapshot("1","2016-07-20")
     sparkSession.stop()


    }



 }