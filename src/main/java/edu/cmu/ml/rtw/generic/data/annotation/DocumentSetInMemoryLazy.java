package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.StoredItemSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.util.MathUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;

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
	 
	public List<DocumentSetInMemoryLazy<E, I>> makePartition(int parts, Random random) {
		double[] distribution = new double[parts];
		String[] names = new String[distribution.length];
		for (int i = 0; i < distribution.length; i++) {
			names[i] = this.name + "_" + String.valueOf(i);
			distribution[i] = 1.0/parts;
		}
	
		return makePartition(distribution, names, random);
	}
	
	public List<DocumentSetInMemoryLazy<E, I>> makePartition(double[] distribution, Random random) {
		String[] names = new String[distribution.length];
		for (int i = 0; i < names.length; i++)
			names[i] = this.name + "_" + String.valueOf(i);
	
		return makePartition(distribution, names, random);
	}
	
	public List<DocumentSetInMemoryLazy<E, I>> makePartition(double[] distribution, String[] names, Random random) {
		List<Entry<String, Pair<Object, E>>> documentList = new ArrayList<Entry<String, Pair<Object, E>>>();
		documentList.addAll(this.items.entrySet());
		
		List<Integer> documentPermutation = new ArrayList<Integer>();
		for (int i = 0; i < documentList.size(); i++)
			documentPermutation.add(i);
		
		documentPermutation = MathUtil.randomPermutation(random, documentPermutation);
		List<DocumentSetInMemoryLazy<E, I>> partition = new ArrayList<DocumentSetInMemoryLazy<E, I>>(distribution.length);
		
		int offset = 0;
		for (int i = 0; i < distribution.length; i++) {
			int partSize = (int)Math.floor(documentList.size()*distribution[i]);
			if (i == distribution.length - 1 && offset + partSize < documentList.size())
				partSize = documentList.size() - offset;
			
			DocumentSetInMemoryLazy<E, I> part = new DocumentSetInMemoryLazy<E, I>(this.storedItems, -1, new Random(), true);
			part.name = names[i];
			
			for (int j = offset; j < offset + partSize; j++) {
				Entry<String, Pair<Object, E>> entry = documentList.get(documentPermutation.get(j));
				part.items.put(entry.getKey(), entry.getValue());
			}
			
			offset += partSize;
			partition.add(part);
		}
		
		return partition;
	} 

	@Override
	public Iterator<E> iterator() {
		return new ItemSetIterator();
	}

	@Override
	public <T> List<T> map(Fn<E, T> fn, int threads, Random r) {
		List<T> results = new ArrayList<T>();
		List<DocumentSetInMemoryLazy<E, I>> partition = this.makePartition(threads, r);
		ThreadMapper<DocumentSetInMemoryLazy<E, I>, Boolean> mapper = new ThreadMapper<DocumentSetInMemoryLazy<E, I>, Boolean>(
			new Fn<DocumentSetInMemoryLazy<E, I>, Boolean>() {
				@Override
				public Boolean apply(DocumentSetInMemoryLazy<E, I> item) {
					for (String documentName : item.items.keySet()) {
						T result = fn.apply(getDocumentByName(documentName));
						synchronized (results) {
							results.add(result);
						}
					}
					
					return true;
				}
			}
		);
		
		mapper.run(partition, threads);
		
		return results;
	}
}
