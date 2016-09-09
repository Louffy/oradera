package com.ucar.growth.analysis.orderanalysis;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
/**
 * Created by zfx on 2016/9/9.
 */
public class Test {
    public static void main(String[] args){
        SparkConf conf = new SparkConf().setAppName("com.ucar.growth.analysis.orderanalysis.Test");
        JavaSparkContext sc = new JavaSparkContext(conf);
        JavaRDD<String> rddseg = sc.textFile("hdfs://namenode01.bi.10101111.com:8020/ml/mlss/citymap/seg2.csv");
        JavaRDD<Integer> r = rddseg.map(item -> 1);
        r.repartition(1).saveAsTextFile("hdfs://namenode01.bi.10101111.com:8020/user/fx.zhang/testResult");
    }
}
