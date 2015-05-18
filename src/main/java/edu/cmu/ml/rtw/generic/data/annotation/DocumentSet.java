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
import java.util.Set;

import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * 
 * @author Bill McDowell
 *
 * @param Document type
 */
public abstract class DocumentSet<D extends Document> implements Collection<D> {
	protected interface DocumentLoader<D> {
		D load(String documentFileName);
	}
	
	private String name;
	
	protected DocumentLoader<D> documentLoader;
	protected String directoryPath;
	
	protected Map<String, Pair<String, D>> fileNamesAndDocuments;
	
	public DocumentSet(String name) {
		this.name = name;
		this.fileNamesAndDocuments = new HashMap<String, Pair<String, D>>();
	}
	
	public String getName() {
		return this.name;
	}
	
	public D getDocumentByName(String name) {
		if (this.fileNamesAndDocuments.containsKey(name)) {
			Pair<String, D> fileNameAndDocument = this.fileNamesAndDocuments.get(name);
			if (fileNameAndDocument.getSecond() == null)
				fileNameAndDocument.setSecond(this.documentLoader.load(fileNameAndDocument.getFirst()));
				
			return fileNameAndDocument.getSecond();
		} else {
			return null;
		}
	}
	
	public Set<String> getDocumentNames() {
		return this.fileNamesAndDocuments.keySet();
	}
	 
	protected abstract DocumentSet<D> makeInstance();
	
	/* FIXME Implement these later 
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
	} */
	
	public boolean saveToJSONDirectory(String directoryPath) {
		for (String name : getDocumentNames()) {
			if (!getDocumentByName(name).saveToJSONFile(new File(directoryPath, name + ".json").getAbsolutePath()))
				return false;
		}
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public static <D extends Document, S extends DocumentSet<D>> S loadFromJSONDirectory(String directoryPath, D genericDocument, S genericDataSet) {
		File directory = new File(directoryPath);
		DocumentSet<D> documentSet = genericDataSet.makeInstance();
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
				if (!file.isDirectory()) {
					tempFiles.add(file);
				}
			}
			
			Collections.sort(tempFiles, new Comparator<File>() { // Ensure determinism
			    public int compare(File o1, File o2) {
			        return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
			    }
			});
			
			documentSet.directoryPath = directoryPath;
			documentSet.documentLoader = new DocumentLoader<D>() {
				@Override
				public D load(String documentFileName) {
					return (D)genericDocument.makeInstanceFromJSONFile((new File(directoryPath, documentFileName)).getAbsolutePath());
				}
			};
			
			for (File file : tempFiles) {
				documentSet.fileNamesAndDocuments.put(file.getName(), new Pair<String, D>(file.getName(), null));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}	
		
		return (S)documentSet;
	}
	
	@Override
	public boolean add(D d) {
		this.fileNamesAndDocuments.put(d.getName(), new Pair<String, D>(null, d));
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
		this.fileNamesAndDocuments.clear();
	}

	@Override
	public boolean contains(Object o) {
		Document d = (Document)o;
		return this.fileNamesAndDocuments.containsKey(d.getName());
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
		return this.fileNamesAndDocuments.isEmpty();
	}

	@Override
	public Iterator<D> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		Document d = (Document)o;
		if (!this.fileNamesAndDocuments.containsKey(d.getName()))
			return false;
		
		this.fileNamesAndDocuments.remove(d.getName());
		
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
		Map<String, Pair<String, D>> retainedDocuments = new HashMap<String, Pair<String, D>>();
		
		for (Object o : c) {
			Document d = (Document)o;
			if (this.fileNamesAndDocuments.containsKey(d.getName()))
				retainedDocuments.put(d.getName(), this.fileNamesAndDocuments.get(d.getName()));
		}
		
		boolean changed = retainedDocuments.size() != this.fileNamesAndDocuments.size();
		this.fileNamesAndDocuments = retainedDocuments;
		
		return changed;
	}

	@Override
	public int size() {
		return this.fileNamesAndDocuments.size();
	}

	@Override
	public Object[] toArray() {
		return this.fileNamesAndDocuments.values().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.fileNamesAndDocuments.values().toArray(a);
	}

}
