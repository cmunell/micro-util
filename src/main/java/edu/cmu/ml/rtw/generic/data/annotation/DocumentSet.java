package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.Set;

import edu.cmu.ml.rtw.generic.data.store.StoredCollection;

public abstract class DocumentSet<E extends Document, I extends E> implements Iterable<E>  {
	protected StoredCollection<I, ?> storedDocuments;
	
	public DocumentSet(StoredCollection<I, ?> storedDocuments) {
		this.storedDocuments = storedDocuments;	
	}
	
	public abstract String getName();
	public abstract E getDocumentByName(String name);
	public abstract Set<String> getDocumentNames();
}
