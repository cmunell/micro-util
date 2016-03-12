package edu.cmu.ml.rtw.generic.data.store;

import edu.cmu.ml.rtw.generic.data.Serializer;

public interface Storage<C extends StoredCollection<?, S>, S> {
	String getName();
	boolean hasCollection(String name);
	boolean renameCollection(String oldName, String newName);
	boolean deleteCollection(String name);
	
	<I> C getCollection(String name);
	<I> C getCollection(String name, Serializer<I, S> serializer);
	<I> C createCollection(String name, Serializer<I, S> serializer);
	<I> Serializer<I, S> getCollectionSerializer(String name);
}
