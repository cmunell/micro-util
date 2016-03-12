package edu.cmu.ml.rtw.generic.data.store;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Serializer;

public class StorageInMemory<S> implements Storage<StoredCollectionInMemory<?, S>, S> {
	private String name;
	private Map<String, StoredCollectionInMemory<?, S>> collections;
	
	
	public StorageInMemory(String name) {
		this.name = name;
		this.collections = new HashMap<String, StoredCollectionInMemory<?, S>>();
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public boolean hasCollection(String name) {
		return this.collections.containsKey(name);
	}

	@Override
	public boolean renameCollection(String oldName, String newName) {
		if (!hasCollection(oldName) || hasCollection(newName))
			return false;
		
		this.collections.put(newName, this.collections.remove(oldName));
		
		return true;
	}

	@Override
	public boolean deleteCollection(String name) {
		if (!hasCollection(name))
			return false;
		this.collections.remove(name);
		return true;
	}

	@Override
	public <I> StoredCollectionInMemory<?, S> getCollection(String name) {
		return this.collections.get(name);
	}

	@Override
	public <I> StoredCollectionInMemory<?, S> getCollection(String name,
			Serializer<I, S> serializer) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public <I> StoredCollectionInMemory<?, S> createCollection(String name,
			Serializer<I, S> serializer) {
		if (hasCollection(name))
			return null;
		
		StoredCollectionInMemory<I, S> collection = new StoredCollectionInMemory<I, S>(name, serializer);
		this.collections.put(name, collection);
		return collection;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <I> Serializer<I, S> getCollectionSerializer(String name) {
		if (!hasCollection(name))
			return null;
		return (Serializer<I, S>)this.collections.get(name).getSerializer();
	}
}
