package apacheSpark;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.config.WriteConfig;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.SparkSession;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class mongoSpark {

    public void writeToMongo(String data){
        JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
        SparkSession sparkForMongo = SparkSession.builder().getOrCreate();
        JavaSparkContext jsc = new JavaSparkContext(sparkForMongo.sparkContext());
        Vector<String> accountsGroups =new Vector<>();
        Map<String,String > writeconf=new HashMap<String,String>();

        JsonArray arr = jsonObject.getAsJsonArray("accounts");
        if(arr!=null && arr.size()!=0){
            System.out.println("Accounts Fuori");
            for (int i = 0; i < arr.size(); i++) {
                System.out.println("Accounts dentro");
                String post_id = arr.get(i).getAsString();
                System.out.println(post_id);
                accountsGroups.add(post_id);
            }
            writeconf.put("database",jsonObject.get("Company").getAsString());
            writeconf.put("collection","accounts");
            writeconf.put("writeConcern.w", "majority");

            WriteConfig writeConfig = WriteConfig.create(jsc).withOptions(writeconf);
            MongoSpark.save(jsc.parallelize(accountsGroups).map((Function<String, Document>) json -> Document.parse(json)),writeConfig);
        }
        JsonArray groups = jsonObject.getAsJsonArray("groups");
        if(groups!=null && groups.size()!=0){

            System.out.println("groups Fuori");
            for (int i = 0; i < groups.size(); i++) {
                System.out.println("groups dentro");
                String post_id = groups.get(i).getAsString();
                System.out.println(post_id);
                accountsGroups.add(post_id);
            }
            writeconf.put("database",jsonObject.get("Company").getAsString());
            writeconf.put("collection","groups");
            writeconf.put("writeConcern.w", "majority");

            WriteConfig writeConfig = WriteConfig.create(jsc).withOptions(writeconf);
            MongoSpark.save(jsc.parallelize(accountsGroups).map((Function<String, Document>) json -> Document.parse(json)),writeConfig);

        }
    }
}
