package apacheSpark;


import org.apache.spark.*;

import org.apache.spark.streaming.*;
import org.apache.spark.streaming.api.java.*;

public class sparkStream {
    public static void main(String[] args) throws InterruptedException {

        // Create a local StreamingContext with two working thread and batch interval of 1 second
        SparkConf conf = new SparkConf().setMaster("local[10]").setAppName("NetworkWordCount")
                .set("spark.mongodb.input.uri", "mongodb://127.0.0.1/mylib.account")
                .set("spark.mongodb.output.uri", "mongodb://127.0.0.1/mylib.account")
                ;
        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(6));

        //Create a DStream that will connect to hostname:port, like localhost:9999
        JavaReceiverInputDStream<String> lines = jssc.receiverStream(new JavaCustomReceiver(8000));
        lines.foreachRDD(rdd->{
                    rdd.foreachPartition(partitionOfRecords -> {
                        mongoSpark m = new mongoSpark();
                        while (partitionOfRecords.hasNext()){
                            m.writeToMongo(partitionOfRecords.next());
                        }
                    });
            }
        );
        // Print the first ten elements of each RDD generated in this DStream to the console
        lines.print();
        jssc.start();              // Start the computation
        jssc.awaitTermination();   // Wait for the computation to terminate
    }
}
