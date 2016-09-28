package com.ucar.growth.analysis.orderanalysis;


import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Hdfs;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.tools.HDFSConcat;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.util.*;

/**
 * Created by zfx on 2016/9/23.
 */
public class WordCount {

    public static class TokenizerMapper extends Mapper<Object,Text,Text,IntWritable> {
        static enum CountersEnum {INPUT_WORDS}
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
        private boolean caseSensitive;
        private Set<String> patternsToSkip = new HashSet<String>();

        private Configuration conf;
        private BufferedReader fis;

        @Override
        public void setup(org.apache.hadoop.mapreduce.Mapper.Context context) throws IOException,InterruptedException{
            conf = context.getConfiguration();
            caseSensitive = conf.getBoolean("wordcount.case.sensitive",true);
            if(conf.getBoolean("wordcount.skip.patterns",true)){
                URI[] patternsURIs = Job.getInstance(conf).getCacheFiles();
                for(URI patternsURI: patternsURIs){
                    Path patternsPath = new Path(patternsURI.getPath());
                    String patternsFileName = patternsPath.getName();
                    parseSkipFile(patternsFileName);
                }
            }


        }
        private void parseSkipFile(String name){
            try{
                fis = new BufferedReader(new FileReader(name));
                String pattern = null;
                while((pattern = fis.readLine()) != null){
                    patternsToSkip.add(pattern);
                }
            }catch (IOException e){
                System.err.println("paresing"+ org.apache.hadoop.util.StringUtils.stringifyException(e));
            }
        }
        @Override
        public void map(Object key, Text value, org.apache.hadoop.mapreduce.Mapper.Context context) throws IOException,InterruptedException{
            String line = (caseSensitive)?value.toString():value.toString().toLowerCase();
            for(String pattern : patternsToSkip){
                line = line.replaceAll(pattern," ");
            }
            StringTokenizer itr = new StringTokenizer(line);
            while(itr.hasMoreTokens()){
                word.set(itr.nextToken());
                context.write(word,one);
                Counter counter = context.getCounter(CountersEnum.class.getName(),CountersEnum.INPUT_WORDS.toString());
                counter.increment(1);
            }
        }
    }

    public static class IntSumReducer extends Reducer<Text,IntWritable,Text,IntWritable>{
        private IntWritable result = new IntWritable();
        public void reduce(Text key,Iterable<IntWritable> values,Context context) throws IOException,InterruptedException{
            int sum = 0;
            for(IntWritable intWritable : values){
                sum += intWritable.get();
            }
            result.set(sum);
            context.write(key,result);
        }
    }

    public static void main(String[] args) throws Exception{
        Configuration conf = new Configuration();
        GenericOptionsParser optionsParser = new GenericOptionsParser(conf,args);
        String[] remainingArgs = optionsParser.getRemainingArgs();
        if(!(remainingArgs.length != 2 || remainingArgs.length != 4)){
            System.err.println("wordcount in out -skip skipPatternFile");
        }
        Job job = Job.getInstance(conf,"wordcount");
        job.setJarByClass(WordCount.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        List<String> otherArgs = new ArrayList<String>();
        for(int i = 0;i < remainingArgs.length;i++){
            if("-skip".equals(remainingArgs[i])){
                job.addCacheFile(new Path(remainingArgs[++i]).toUri());
                job.getConfiguration().setBoolean("wordcount.skip.patterns",true);
            }else{
                otherArgs.add(remainingArgs[i]);
            }
        }
        FileInputFormat.addInputPath(job,new Path(otherArgs.get(0)));
        FileSystem hdfs = FileSystem.get(conf);
        if(hdfs.exists(new Path(otherArgs.get(1))))
            hdfs.delete(new Path(otherArgs.get(1)),true);
        FileOutputFormat.setOutputPath(job,new Path(otherArgs.get(1)));

        System.exit(job.waitForCompletion(true)?0:1);
    }
}
