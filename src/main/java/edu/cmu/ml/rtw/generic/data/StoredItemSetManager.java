package edu.cmu.ml.rtw.generic.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.store.Storage;
import edu.cmu.ml.rtw.generic.data.store.StorageFileSystem;
import edu.cmu.ml.rtw.generic.data.store.StorageInMemory;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.util.Properties;

public class StoredItemSetManager {
	private Map<String, Map<String, StoredItemSetInMemoryLazy<?, ?>>> itemSets;
	private Map<String, Storage<?, ?>> storages;
	

	public StoredItemSetManager(Properties properties, Map<String, Serializer<?, ?>> serializers) {
		this.storages = new HashMap<String, Storage<?, ?>>();
		this.itemSets = new HashMap<String, Map<String, StoredItemSetInMemoryLazy<?, ?>>>();
		Map<String, String> fsBsonStorage = properties.getFileSystemStorageBSONDirectories();
		for (Entry<String, String> entry : fsBsonStorage.entrySet()) {
			this.storages.put(entry.getKey(), new StorageFileSystem<Document>(entry.getKey(), entry.getValue(), serializers));
		}	
		
		Map<String, String> fsStringStorage = properties.getFileSystemStorageStringDirectories();
		for (Entry<String, String> entry : fsStringStorage.entrySet()) {
			this.storages.put(entry.getKey(), new StorageFileSystem<String>(entry.getKey(), entry.getValue(), serializers));
		}
		
		this.storages.put("StringMemory", new StorageInMemory<String>("StringMemory"));
		this.storages.put("BSONMemory", new StorageInMemory<Document>("BSONMemory"));
	}
	
	public <E> E resolveStoreReference(StoreReference reference) {
		return resolveStoreReference(reference, false);
	}
	
	@SuppressWarnings("unchecked")
	public <E> E resolveStoreReference(StoreReference reference, boolean keepInMemory) {
		StoredItemSetInMemoryLazy<E, ?> itemSet = (StoredItemSetInMemoryLazy<E, ?> )getItemSet(reference.getStorageName(), reference.getCollectionName(), false, null);
		if (itemSet == null)
			return null;
		
		// FIXME For now, this just assumes that reference has single index matching item set 
		return itemSet.getItemByIndex(reference.getIndexValues().get(0).toString(), keepInMemory);
	}
	
	public <E, I extends E> StoredItemSetInMemoryLazy<E, I> getItemSet(String collectionName) {
		if (this.storages.size() < 1)
			throw new IllegalArgumentException();
		
		String storageName = this.storages.keySet().iterator().next();
		return getItemSet(storageName, collectionName, false, null);
	}
	
	public <E, I extends E> StoredItemSetInMemoryLazy<E, I> getItemSet(String storageName, String collectionName) {
		return getItemSet(storageName, collectionName, false, null);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <E, I extends E> StoredItemSetInMemoryLazy<E, I> getItemSet(String storageName, String collectionName, boolean createIfAbsent, Serializer<I, ?> serializer) {
		if (this.itemSets.containsKey(storageName) && this.itemSets.get(storageName).containsKey(collectionName))
			return (StoredItemSetInMemoryLazy<E, I>)this.itemSets.get(storageName).get(collectionName);
		
		if (!this.storages.containsKey(storageName))
			return null;
		
		Storage<?, ?> storage = this.storages.get(storageName);
		
		StoredCollection<I, ?> collection = null;
		if (!storage.hasCollection(collectionName)) {
			if (createIfAbsent)
				collection = storage.createCollection(collectionName, (Serializer)serializer);
			if (collection == null)
				return null;
		} else {
			if (serializer != null)
				collection = (StoredCollection<I, ?>)storage.getCollection(collectionName, (Serializer)serializer);
			else
				collection = (StoredCollection<I, ?>)storage.getCollection(collectionName);
		}
		
		StoredItemSetInMemoryLazy<E, I> itemSet = new StoredItemSetInMemoryLazy<E, I>(collection);
		
		if (!this.itemSets.containsKey(storageName))
			this.itemSets.put(storageName, new HashMap<String, StoredItemSetInMemoryLazy<?, ?>>());
		
		this.itemSets.get(storageName).put(collectionName, itemSet);
		
		return itemSet;
	}
	
	public Storage<?, ?> getStorage(String storageName) {
		return this.storages.get(storageName);
	}
}
