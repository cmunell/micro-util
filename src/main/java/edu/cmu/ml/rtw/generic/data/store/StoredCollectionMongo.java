package edu.cmu.ml.rtw.generic.data.store;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Projections;

import edu.cmu.ml.rtw.generic.data.Serializer;

public class StoredCollectionMongo<I> extends StoredCollection<I, Document> {
	private class MongoIterator implements Iterator<I> {
		private MongoCursor<Document> cursor;
		
		public MongoIterator(MongoCursor<Document> cursor) {
			this.cursor = cursor;
		}
		
		@Override
		public boolean hasNext() {
			return this.cursor.hasNext();
		}

		@Override
		public I next() {
			return getSerializer().deserialize(this.cursor.next());
		}

		@Override
		public void remove() {
			this.cursor.remove();
		}
	}
	
	private MongoCollection<Document> collection;
	private StorageMongo storage;
	
	private Serializer<I, Document> serializer;
	
	public StoredCollectionMongo(String name, MongoCollection<Document> collection, StorageMongo storage) {
		super(name);
		this.collection = collection;
		this.storage = storage;
	}
	
	public StoredCollectionMongo(String name, MongoCollection<Document> collection, Serializer<I, Document> serializer) {
		super(name);
		this.collection = collection;
		this.serializer = serializer;
	}	

	@Override
	public Iterator<I> iterator() {
		return new MongoIterator(this.collection.find().iterator());
	}

	@Override
	public Serializer<I, Document> getSerializer() {
		if (this.serializer == null)
			this.serializer = this.storage.getCollectionSerializer(this.name);
		return this.serializer;
	}

	// FIXME Possibly non-deterministic results if limit is imposed
	@Override
	public synchronized Set<String> getIndex(String indexField, int limit) {
		Set<String> values = new HashSet<String>();
		
		FindIterable<Document> index = null;
		if (limit > 0) {
			index = this.collection.find().limit(limit).projection(Projections.include(indexField));
		} else {
			index = this.collection.find().projection(Projections.include(indexField));
		}
		for (Document document : index) {
			values.add(document.get(indexField).toString());
		}
		
		return values;
	}

	@Override
	public synchronized List<I> getItemsByIndex(String indexField, Object indexValue) {
		List<I> items = new ArrayList<I>();
		for (Document document : this.collection.find(new Document().append(indexField, indexValue))) {
			items.add(getSerializer().deserialize(document));
		}
		return items;
	}

	@Override
	public synchronized List<I> getItemsByIndices(List<String> indexFields, List<Object> indexValues) {
		if (indexFields.size() != indexValues.size())
			return null;
		
		Document filter = new Document();
		for (int i = 0; i < indexFields.size(); i++)
			filter.append(indexFields.get(i), indexValues.get(i));
		
		List<I> items = new ArrayList<I>();
		for (Document document : this.collection.find(filter)) {
			items.add(getSerializer().deserialize(document));
		}
		
		return items;
	}

	@Override
	public boolean addItem(I item) {		
		this.collection.insertOne(getSerializer().serialize(item));
		return true;
	}

	@Override
	public boolean addItems(List<I> items) {
		List<Document> documents = new ArrayList<Document>();
		for (I item : items)
			documents.add(getSerializer().serialize(item));
		this.collection.insertMany(documents);
		return true;
	}
}
