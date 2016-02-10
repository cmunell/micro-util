package edu.cmu.ml.rtw.generic.data.annotation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.util.MathUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;

public class DocumentSetInMemoryLazy<E extends Document, I extends E> extends DocumentSet<E, I> {
	private class DocumentSetIterator implements Iterator<E> {
		private Iterator<String> nameIterator;
		
		public DocumentSetIterator() {
			this.nameIterator = DocumentSetInMemoryLazy.this.documents.keySet().iterator();
		}
		
		@Override
		public boolean hasNext() {
			return this.nameIterator.hasNext();
		}

		@Override
		public E next() {
			return DocumentSetInMemoryLazy.this.getDocumentByName(this.nameIterator.next(), false);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	private ConcurrentSkipListMap<String, Pair<Object, E>> documents;
	private String nameIndexField;
	private String name;
	
	public DocumentSetInMemoryLazy(StoredCollection<I, ?> storedDocuments) {
		this(storedDocuments, -1, false);
	}
	
	public DocumentSetInMemoryLazy(StoredCollection<I, ?> storedDocuments, int sizeLimit) {
		this(storedDocuments, sizeLimit, false);
	}
	
	public DocumentSetInMemoryLazy(StoredCollection<I, ?> storedDocuments, int sizeLimit, boolean initEmpty) {
		super(storedDocuments);
		this.documents = new ConcurrentSkipListMap<String, Pair<Object, E>>();
		this.nameIndexField = this.storedDocuments.getSerializer().getIndices().get(0).getField();
		
		if (!initEmpty) {
			Set<String> documentNames = this.storedDocuments.getIndex(this.nameIndexField, sizeLimit);
			for (String documentName : documentNames)
				this.documents.put(documentName, new Pair<Object, E>(new Object(), null));
		}
		
		this.name = this.storedDocuments.getName();
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public E getDocumentByName(String name) {
		return getDocumentByName(name, true);
	}
	
	public E getDocumentByName(String name, boolean keepInMemory) {
		if (this.documents.containsKey(name)) {
			Pair<Object, E> lockAndDocument = this.documents.get(name);
		
			if (keepInMemory) {
				synchronized (lockAndDocument.getFirst()) {
					if (lockAndDocument.getSecond() == null)
						lockAndDocument.setSecond(this.storedDocuments.getFirstItemByIndex(this.nameIndexField, name));
				}
				
				return lockAndDocument.getSecond();
			} else {
				if (lockAndDocument.getSecond() == null)
					return this.storedDocuments.getFirstItemByIndex(this.nameIndexField, name);
				else
					return lockAndDocument.getSecond();
			}
		} else {
			return null;
		}
	}
	
	@Override
	public Set<String> getDocumentNames() {
		return this.documents.keySet();
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
		documentList.addAll(this.documents.entrySet());
		
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
			
			DocumentSetInMemoryLazy<E, I> part = new DocumentSetInMemoryLazy<E, I>(this.storedDocuments, -1, true);
			part.name = names[i];
			
			for (int j = offset; j < offset + partSize; j++) {
				Entry<String, Pair<Object, E>> entry = documentList.get(documentPermutation.get(j));
				part.documents.put(entry.getKey(), entry.getValue());
			}
			
			offset += partSize;
			partition.add(part);
		}
		
		return partition;
	} 

	@Override
	public Iterator<E> iterator() {
		return new DocumentSetIterator();
	}

	@Override
	public <T> List<T> map(Fn<E, T> fn, int threads, Random r) {
		List<T> results = new ArrayList<T>();
		List<DocumentSetInMemoryLazy<E, I>> partition = this.makePartition(threads, r);
		ThreadMapper<DocumentSetInMemoryLazy<E, I>, Boolean> mapper = new ThreadMapper<DocumentSetInMemoryLazy<E, I>, Boolean>(
			new Fn<DocumentSetInMemoryLazy<E, I>, Boolean>() {
				@Override
				public Boolean apply(DocumentSetInMemoryLazy<E, I> item) {
					List<T> results = new ArrayList<T>();
					for (String documentName : item.documents.keySet()) {
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
