package com.ucar.growth.analysis.orderanalysis.driverdata.util;


import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.deploy.yarn.Client;
import org.apache.spark.deploy.yarn.ClientArguments;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by zfx on 2016/10/14.
 */
public class SubmitJob {

    public static void submit(String[] arguments) throws Exception {


        String[] args = new String[] {

                // memory for driver (optional)
                "--driver-memory",
                "1000M",

                // path to your application's JAR file
                // required in yarn-cluster mode


                // name of your application's main class (required)
                "--class",
                "org.dataalgorithms.bonus.friendrecommendation.spark.SparkFriendRecommendation",


                // argument 4 to your Spark program (SparkFriendRecommendation)
                // this is a helper argument to create a proper JavaSparkContext object
                // make sure that you create the following in SparkFriendRecommendation program
                // ctx = new JavaSparkContext("yarn-cluster", "SparkFriendRecommendation");
                "--arg",
                "yarn-cluster"
        };

        // create a Hadoop Configuration object
        Configuration config = new Configuration();

        // identify that you will be using Spark as YARN mode
        System.setProperty("SPARK_YARN_MODE", "true");

        // create an instance of SparkConf object
        SparkConf sparkConf = new SparkConf();

        // create ClientArguments, which will be passed to Client
        ClientArguments cArgs = new ClientArguments(args, sparkConf);

        // create an instance of yarn Client client
        Client client = new Client(cArgs, config, sparkConf);

        // submit Spark job to YARN
        //client.run();
    }
    public static void main(String[] args){
        try {
            Configuration conf = new Configuration();
            conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
            FileSystem fs = FileSystem.get(new java.net.URI("hdfs://namenode01.bi.10101111.com:8020"), conf);
            Path file = new Path("hdfs://namenode01.bi.10101111.com:8020/user/fx.zhang/OrderDispatchAnalysis/data/order/27315631550415/part-00000");
            FSDataInputStream getIt = fs.open(file);
            BufferedReader d = new BufferedReader(new InputStreamReader(getIt));
            String s = "";
            while ((s = d.readLine()) != null) {
                System.out.println(s);
            }
            d.close();
            fs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
