package edu.cmu.ml.rtw.generic.data.store;

import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoNamespace;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.Serializer.Index;

public class StorageMongo implements Storage<StoredCollectionMongo<?>, Document> {
	private static final String META_COLLECTION = "meta";
	
	private Map<String, Serializer<?, ?>> serializers;
	private MongoClient client;
	private MongoDatabase database;
	private String name;
	
	public StorageMongo(String name, String host, String database, Map<String, Serializer<?, ?>> serializers) {
		this.client = new MongoClient(host);
		this.database = this.client.getDatabase(database);
		this.serializers = serializers;
		this.name = name;
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public boolean hasCollection(String name) {
		MongoIterable<String> collections = this.database.listCollectionNames();
		for (String collection : collections)
			if (name.equals(collection))
				return true;
		return false;
	}

	@Override
	public <I> StoredCollectionMongo<I> getCollection(String name) {
		if (!hasCollection(name))
			return null;
		
		return new StoredCollectionMongo<I>(name, this.database.getCollection(name), this);	
	}
	
	@Override
	public <I> StoredCollectionMongo<?> getCollection(String name,
			Serializer<I, Document> serializer) {
		if (!hasCollection(name))
			return null;
		
		return new StoredCollectionMongo<I>(name, this.database.getCollection(name), serializer);	
	}

	@Override
	public <I> StoredCollectionMongo<I> createCollection(String name, Serializer<I, Document> serializer) {
		if (hasCollection(name))
			return null;
		
		this.database.createCollection(name);
		
		List<Index<I>> indices = serializer.getIndices();
		Document indexDocument = new Document();
		for (Index<I> index : indices) {
			indexDocument.append(index.getField(), 1);
		}
		MongoCollection<Document> collection = this.database.getCollection(name);
		collection.createIndex(indexDocument);
		
		if (!hasCollection(META_COLLECTION)) {
			this.database.createCollection(META_COLLECTION);
		}
		
		MongoCollection<Document> metaCollection = this.database.getCollection(META_COLLECTION);
		metaCollection.insertOne((new Document()).append("collection", name).append("serializer", serializer.getName()));
		
		return new StoredCollectionMongo<I>(name, collection, this, serializer);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <I> Serializer<I, Document> getCollectionSerializer(String name) {
		FindIterable<Document> metaCollection = this.database.getCollection(META_COLLECTION).find();
		for (Document metaDocument : metaCollection) {
			if (metaDocument.getString("collection").equals(name)) {
				return (Serializer<I, Document>)this.serializers.get(metaDocument.getString("serializer")); 
			}
		}
		
		return null;
	}

	@Override
	public boolean renameCollection(String oldName, String newName) {
		if (!hasCollection(oldName) || hasCollection(newName) || oldName.equals(newName))
			return false;
		
		this.database.getCollection(oldName).renameCollection(new MongoNamespace(this.database.getName(), newName));
		
		UpdateResult result = this.database.getCollection(META_COLLECTION)
								.updateOne(new Document("collection", oldName),
										new Document("$set", new Document("collection", newName)));
		
		return result.getModifiedCount() == 1;
	}

	@Override
	public boolean deleteCollection(String name) {
		if (!hasCollection(name))
			return false;
		
		this.database.getCollection(name).drop();
		
		DeleteResult result = this.database.getCollection(META_COLLECTION).deleteOne(new Document("collection", name));
		
		return result.getDeletedCount() == 1;
	}
}
