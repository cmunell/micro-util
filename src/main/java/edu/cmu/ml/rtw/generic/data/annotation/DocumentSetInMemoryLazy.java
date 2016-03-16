package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.StoredItemSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;

public class DocumentSetInMemoryLazy<E extends Document, I extends E> extends StoredItemSetInMemoryLazy<E, I> implements DocumentSet<E, I> {
	public DocumentSetInMemoryLazy(StoredItemSetInMemoryLazy<E, I> itemSet) {
		this(itemSet.getStoredItems());
	}
	
	public DocumentSetInMemoryLazy(StoredCollection<I, ?> storedDocuments) {
		this(storedDocuments, -1);
	}
	
	public DocumentSetInMemoryLazy(StoredCollection<I, ?> storedDocuments, int sizeLimit) {
		this(storedDocuments, sizeLimit, new Random(), false);
	}
	
	public DocumentSetInMemoryLazy(StoredCollection<I, ?> storedDocuments, int sizeLimit, Random r, boolean initEmpty) {
		super(storedDocuments, sizeLimit, r, initEmpty);
	}
	
	@Override
	public E getDocumentByName(String name) {
		return getDocumentByName(name, true);
	}
	
	public E getDocumentByName(String name, boolean keepInMemory) {
		return getItemByIndex(name, keepInMemory);
	}
	
	@Override
	public Set<String> getDocumentNames() {
		return this.items.keySet();
	}

	@Override
	public Iterator<E> iterator() {
		return new ItemSetIterator();
	}
}
