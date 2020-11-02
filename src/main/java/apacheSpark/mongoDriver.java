package apacheSpark;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

public class mongoDriver{
    private void updateOrInsert(JsonArray arr, MongoCollection<Document> coll){
        for (int i = 0; i < arr.size(); i++) {
            JsonObject element = new JsonParser().parse(arr.get(i).getAsString()).getAsJsonObject();
            System.out.println(element.get("dn").getAsString());
            Document accToUp = Document.parse(arr.get(i).getAsString());
            // replace or insert a document
            ReplaceOptions options=new ReplaceOptions().upsert(true);
            coll.replaceOne(Filters.eq("dn", element.get("dn").getAsString()), accToUp,options);
        }
    }
    public void writeToMongo(String data){
        JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
        MongoClient mongoClient = MongoClients.create("mongodb://127.0.0.1");
        MongoDatabase database = mongoClient.getDatabase(jsonObject.get("Company").getAsString());

        System.out.println("---------------PAGE------------------");
        JsonArray arr = jsonObject.getAsJsonArray("accounts");
        if(arr!=null && arr.size()!=0){
            System.out.println("account");
            MongoCollection<Document> coll = database.getCollection("accounts", Document.class);
            updateOrInsert(arr,coll);
        }
        JsonArray groups = jsonObject.getAsJsonArray("groups");
        if(groups!=null && groups.size()!=0){
            System.out.println("group");
            MongoCollection<Document> coll = database.getCollection("groups", Document.class);
            updateOrInsert(groups,coll);
        }
    }
}