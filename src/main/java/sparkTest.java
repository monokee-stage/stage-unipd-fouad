
import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import org.bson.Document;

import static java.util.Arrays.asList;
import java.util.List;


public class sparkTest {
    public static void main(final String[] args) throws InterruptedException {
        /* Create the SparkSession.
         * If config arguments are passed from the command line using --conf,
         * parse args for the values to set.
         */

        SparkSession spark = SparkSession.builder()
                .master("local")
                .appName("MongoSparkConnectorIntro")
                .config("spark.mongodb.input.uri", "mongodb://127.0.0.1/test.myCollection")
                .config("spark.mongodb.output.uri", "mongodb://127.0.0.1/test.myCollection")
                .getOrCreate();

        JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());

        // Create a RDD of 10 documents
        JavaRDD<Document> documents = jsc.parallelize(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)).map
                (new Function<Integer, Document>() {
                    public Document call(final Integer i) throws Exception {
                        return Document.parse("{test: " + i + "}");
                    }
                });

        /*Start Example: Save data from RDD to MongoDB*****************/
        MongoSpark.save(documents);
        /*End Example**************************************************/

        // Add Sample Data
        List<String> characters = asList(
                "{'name': 'Bilbo Baggins', 'age': 50}",
                "{'name': 'Gandalf', 'age': 1000}",
                "{'name': 'Thorin', 'age': 195}",
                "{'name': 'Balin', 'age': 178}",
                "{'name': 'Kíli', 'age': 77}",
                "{'name': 'Dwalin', 'age': 169}",
                "{'name': 'Óin', 'age': 167}",
                "{'name': 'Glóin', 'age': 158}",
                "{'name': 'Fíli', 'age': 82}",
                "{'name': 'Bombur'}"
        );

        MongoSpark.save(jsc.parallelize(characters).map(new Function<String, Document>() {
            public Document call(final String json) throws Exception {
                return Document.parse(json);
            }
        }));

        /*Start Example: Read data from MongoDB************************/
        JavaMongoRDD<Document> rdd = MongoSpark.load(jsc);
        /*End Example**************************************************/

        // Analyze data from MongoDB
        System.out.println(rdd.count());
        System.out.println(rdd.first().toJson());
        // Load inferring schema
        Dataset<Row> df = MongoSpark.load(jsc).toDF();
        df.printSchema();
        df.show();

        // SQL
        df.createOrReplaceTempView("characters");
        Dataset<Row> centenarians = spark.sql("SELECT name, age FROM characters WHERE age >= 100");
       // centenarians.show();

        MongoSpark.write(centenarians).option("collection", "hundredClub").mode("overwrite").save();
        Dataset<Row> temp=spark.read().format("com.mongodb.spark.sql.DefaultSource").option("uri", "mongodb://127.0.0.1/test.hundredClub").load();
        temp.show();
        // Drop database
      /*  MongoConnector.apply(jsc.sc()).withDatabaseDo(ReadConfig.create(spark), new Function<MongoDatabase, Void>() {

            public Void call(final MongoDatabase db) throws Exception {
                db.drop();
                return null;
            }
        });*/
        jsc.close();

        System.out.println("finished");
    }
}
