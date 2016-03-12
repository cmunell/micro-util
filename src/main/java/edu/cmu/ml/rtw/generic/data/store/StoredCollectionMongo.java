package edu.cmu.ml.rtw.generic.data.store;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Projections;

import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.Serializer.Index;

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
			Document doc = this.cursor.next();
			return getSerializer().deserialize(doc, getStoreReference(doc));
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
	
	public StoredCollectionMongo(String name, MongoCollection<Document> collection, StorageMongo storage, Serializer<I, Document> serializer) {
		super(name);
		this.collection = collection;
		this.serializer = serializer;
		this.storage = storage;
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

	// FIXME Possibly non-deterministic results if limit is imposed..  Use random object to ensure determinism
	@Override
	public synchronized Set<String> getIndex(String indexField, int limit, Random r) {
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
	public List<I> getItemsByIndex(String indexField, Object indexValue) {
		List<I> items = new ArrayList<I>();
		

		List<Document> docs = new ArrayList<Document>();
		synchronized (this) {
			for (Document doc : this.collection.find(new Document().append(indexField, indexValue))) {
				docs.add(doc);
			}
		}
		
		for (Document doc : docs) {
			items.add(getSerializer().deserialize(doc));
		}
		return items;
	}

	@Override
	public List<I> getItemsByIndices(List<String> indexFields, List<Object> indexValues) {
		if (indexFields.size() != indexValues.size())
			return null;
		
		Document filter = new Document();
		for (int i = 0; i < indexFields.size(); i++)
			filter.append(indexFields.get(i), indexValues.get(i));
		
		List<Document> docs = new ArrayList<Document>();
		synchronized (this) {
			for (Document doc : this.collection.find(filter)) {
				docs.add(doc);
			}
		}
		
		List<I> items = new ArrayList<I>();
		for (Document doc : docs) {
			items.add(getSerializer().deserialize(doc, getStoreReference(doc)));
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
	
	private StoreReference getStoreReference(Document document) {
		List<String> fields = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();
		List<Index<I>> indices = getSerializer().getIndices();
		
		for (int i = 0; i < indices.size(); i++) {
			fields.add(indices.get(i).getField());
			values.add(document.get(indices.get(i).getField()));
		}
		
		if (this.storage == null) {
			return new StoreReference(null, this.name, fields, values);
		} else {
			return new StoreReference(this.storage.getName(), this.name, fields, values);
		}
	}

	@Override
	public List<BufferedReader> getReadersByIndex(String indexField,
			Object indexValue) {
		// FIXME Implement
		throw new UnsupportedOperationException();
	}

	@Override
	public List<BufferedReader> getReadersByIndices(List<String> indexFields,
			List<Object> indexValues) {
		// FIXME Implement
		throw new UnsupportedOperationException();
	}
}
