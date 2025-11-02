package g2.g2_gp_project.mongo;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoConnectionTestStandalone {
    public static void main(String[] args) {
    String uri = "mongodb://127.0.0.1:27017";
    String dbName = "retail_db";
    String collectionName = "transactions";

        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase db = client.getDatabase(dbName);
            MongoCollection<Document> col = db.getCollection(collectionName);
            long count = col.countDocuments();
            System.out.println("MongoDB connection successful. Invoice count: " + count);
        } catch (Exception e) {
            System.err.println("MongoDB connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}