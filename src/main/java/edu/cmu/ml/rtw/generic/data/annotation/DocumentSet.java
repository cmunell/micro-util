package edu.cmu.ml.rtw.generic.data.annotation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import edu.cmu.ml.rtw.generic.util.MathUtil;

/**
 * 
 * @author Bill McDowell
 *
 * @param Document type
 */
public abstract class DocumentSet<D extends Document> implements Collection<D> {
	private String name;
	protected Map<String, D> documents;
	
	public DocumentSet(String name) {
		this.documents = new HashMap<String, D>();
	}
	
	public String getName() {
		return this.name;
	}
	
	protected abstract DocumentSet<D> makeInstance(String name);
	
	public List<DocumentSet<D>> makePartition(int parts, Random random) {
		double[] distribution = new double[parts];
		String[] names = new String[distribution.length];
		for (int i = 0; i < distribution.length; i++) {
			names[i] = String.valueOf(i);
			distribution[i] = 1.0/parts;
		}
	
		return makePartition(distribution, names, random);
	}
	
	public List<DocumentSet<D>> makePartition(double[] distribution, Random random) {
		String[] names = new String[distribution.length];
		for (int i = 0; i < names.length; i++)
			names[i] = String.valueOf(i);
	
		return makePartition(distribution, names, random);
	}
	
	public List<DocumentSet<D>> makePartition(double[] distribution, String[] names, Random random) {
		List<D> documentList = new ArrayList<D>();
		documentList.addAll(this.documents.values());
		List<Integer> documentPermutation = new ArrayList<Integer>();
		for (int i = 0; i < documentList.size(); i++)
			documentPermutation.add(i);
		
		documentPermutation = MathUtil.randomPermutation(random, documentPermutation);
		List<DocumentSet<D>> partition = new ArrayList<DocumentSet<D>>(distribution.length);
		
		int offset = 0;
		for (int i = 0; i < distribution.length; i++) {
			int partSize = (int)Math.floor(documentList.size()*distribution[i]);
			if (i == distribution.length - 1 && offset + partSize < documentList.size())
				partSize = documentList.size() - offset;
			
			DocumentSet<D> part = makeInstance(names[i]);
			for (int j = offset; j < offset + partSize; j++) {
				part.add(documentList.get(documentPermutation.get(j)));
			}
			
			offset += partSize;
			partition.add(part);
		}
		
		return partition;
	} 
	
	public boolean saveToJSONDirectory(String directoryPath) {
		for (D document : this.documents.values()) {
			if (!document.saveToJSONFile(new File(directoryPath, document.getName() + ".json").getAbsolutePath()))
				return false;
		}
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public static <D extends Document, S extends DocumentSet<D>> S loadFromJSONDirectory(String name, String directoryPath, D genericDocument, S genericDataSet) {
		File directory = new File(directoryPath);
		DocumentSet<D> documentSet = genericDataSet.makeInstance(name);
		try {
			if (!directory.exists() || !directory.isDirectory())
				throw new IllegalArgumentException("Invalid directory: " + directory.getAbsolutePath());
			
			List<File> files = new ArrayList<File>();
			files.addAll(Arrays.asList(directory.listFiles()));
			int numTopLevelFiles = files.size();
			for (int i = 0; i < numTopLevelFiles; i++)
				if (files.get(i).isDirectory())
					files.addAll(Arrays.asList(files.get(i).listFiles()));
			
			List<File> tempFiles = new ArrayList<File>();
			for (File file : files) {
				if (!file.isDirectory() && file.getName().endsWith(".json")) {
					tempFiles.add(file);
				}
			}
			
			Collections.sort(tempFiles, new Comparator<File>() { // Ensure determinism
			    public int compare(File o1, File o2) {
			        return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
			    }
			});
			
			for (File file : tempFiles) {
				documentSet.add((D)genericDocument.makeInstanceFromJSONFile(file.getAbsolutePath()));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}	
		
		return (S)documentSet;
	}
	
	@Override
	public boolean add(D d) {
		this.documents.put(d.getName(), d);
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends D> c) {
		for (D d : c)
			if (!add(d))
				return false;
		return true;
	}

	@Override
	public void clear() {
		this.documents.clear();
	}

	@Override
	public boolean contains(Object o) {
		Document d = (Document)o;
		return this.documents.containsKey(d.getName());
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o))
				return false;
		}

		return true;
	}

	@Override
	public boolean isEmpty() {
		return this.documents.isEmpty();
	}

	@Override
	public Iterator<D> iterator() {
		return this.documents.values().iterator();
	}

	@Override
	public boolean remove(Object o) {
		Document d = (Document)o;
		if (!this.documents.containsKey(d.getName()))
			return false;
		
		this.documents.remove(d.getName());
		
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		for (Object o : c)
			if (!remove(o))
				return false;
		return true; 
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		Map<String, D> retainedDocuments = new HashMap<String, D>();
		
		for (Object o : c) {
			Document d = (Document)o;
			if (this.documents.containsKey(d.getName()))
				retainedDocuments.put(d.getName(), this.documents.get(d.getName()));
		}
		
		boolean changed = retainedDocuments.size() != this.documents.size();
		this.documents = retainedDocuments;
		
		return changed;
	}

	@Override
	public int size() {
		return this.documents.size();
	}

	@Override
	public Object[] toArray() {
		return this.documents.values().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.documents.values().toArray(a);
	}

}
