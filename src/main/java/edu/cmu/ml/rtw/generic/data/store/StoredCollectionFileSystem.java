package edu.cmu.ml.rtw.generic.data.store;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.io.Files;

import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.util.FileUtil;

public class StoredCollectionFileSystem<I, S> extends StoredCollection<I, S> {
	private File directory;
	private StorageFileSystem<S> storage;
	
	private Serializer<I, S> serializer;
	
	public StoredCollectionFileSystem(String name, File directory, StorageFileSystem<S> storage) {
		super(name);
		this.directory = directory;
		this.storage = storage;
	}

	public StoredCollectionFileSystem(String name, File directory, Serializer<I,S> serializer) {
		super(name);
		this.directory = directory;
		this.serializer = serializer;
	}
	
	public StoredCollectionFileSystem(String name, File directory, StorageFileSystem<S> storage, Serializer<I,S> serializer) {
		super(name);
		this.directory = directory;
		this.storage = storage;
		this.serializer = serializer;
	}
	
	@Override
	public Iterator<I> iterator() {
		return Files.fileTreeTraverser()
				.preOrderTraversal(this.directory)
				.filter(new Predicate<File>() { public boolean apply(File file) { return file.isFile(); }})
				.transform(
					new Function<File, I>() {
						@Override
						public I apply(File file) {
							return getSerializer().deserializeFromString(FileUtil.readFile(file), getStoreReference(file));
						}
					}
				).iterator();
	}

	@Override
	public Serializer<I, S> getSerializer() {
		if (this.serializer == null)
			this.serializer = this.storage.getCollectionSerializer(this.name);
		return this.serializer;
	}

	@Override
	public Set<String> getIndex(String indexField, int limit, Random r) {
		int indexNum = getIndexNumber(indexField);
		
		if (indexNum < 0)
			return null;
		
		return getIndex(this.directory, 0, indexNum, new HashSet<String>(), limit, r);
	}
	
	private Set<String> getIndex(File curIndexDir, int curIndexNum, int targetIndexNum, Set<String> values, int limit, Random r) {
		if (curIndexNum == targetIndexNum) {
			if (limit > 0) {
				String[] curValues = FileUtil.listFileNamesRandomOrder(curIndexDir, r);
				for (int i = 0; i < curValues.length; i++) {
					if (values.size() >= limit)
						break;
					values.add(curValues[i]);
				}
				
				return values;
			} else {
				values.addAll(Arrays.asList(FileUtil.listFileNamesRandomOrder(curIndexDir, r)));
			}
		} else {
			for (File file : FileUtil.listFilesRandomOrder(curIndexDir, r)) {
				if (file.isDirectory()) {
					getIndex(file, curIndexNum + 1, targetIndexNum, values, limit, r);
					if (limit > 0 && values.size() >= limit)
						return values;
				}
			}
		}

		return values;
	}

	@Override
	public List<I> getItemsByIndex(String indexField, Object indexValue) {
		List<String> indexFields = new ArrayList<String>();
		List<Object> indexValues = new ArrayList<Object>();
		indexFields.add(indexField);
		indexValues.add(indexValue);
		return getItemsByIndices(indexFields, indexValues);
	}

	@Override
	public List<I> getItemsByIndices(List<String> indexFields, List<Object> indexValues) {
		if (indexFields.size() != indexValues.size())
			return null;
		
		TreeMap<Integer, String> constrainedIndices = new TreeMap<Integer, String>();
		for (int i = 0; i < indexFields.size(); i++) {
			constrainedIndices.put(getIndexNumber(indexFields.get(i)), transformIndexValue(indexValues.get(i)));
		}
	
		return getItemsByIndices(this.directory, 0, constrainedIndices, new ArrayList<I>());
	}
	
	private List<I> getItemsByIndices(File curIndexDir, int curIndexNum, TreeMap<Integer, String> constrainedIndices, List<I> items) {
		File[] constrainedIndex = null;
		if (constrainedIndices.containsKey(curIndexNum)) {
			File constrainedIndexFile = new File(curIndexDir.getAbsolutePath(), constrainedIndices.get(curIndexNum));
			if (!constrainedIndexFile.exists())
				return items;
			else 
				constrainedIndex = new File[] { constrainedIndexFile };
		} else {
			constrainedIndex = curIndexDir.listFiles();
		}
		
		Serializer<I, S> serializer = getSerializer(); 
		for (File file : constrainedIndex) {
			if (file.isDirectory()) {
				getItemsByIndices(file, curIndexNum + 1, constrainedIndices, items);
			} else if (file.isFile() && (constrainedIndices.size() == 0 || curIndexNum >= constrainedIndices.lastKey())) {
				items.add(serializer.deserializeFromString(FileUtil.readFile(file), getStoreReference(file)));
			}
		}
		
		return items;
	}

	@Override
	public List<BufferedReader> getReadersByIndex(String indexField,
			Object indexValue) {
		List<String> indexFields = new ArrayList<String>();
		List<Object> indexValues = new ArrayList<Object>();
		indexFields.add(indexField);
		indexValues.add(indexValue);
		return getReadersByIndices(indexFields, indexValues);
	}

	@Override
	public List<BufferedReader> getReadersByIndices(List<String> indexFields,
			List<Object> indexValues) {
		if (indexFields.size() != indexValues.size())
			return null;
		
		TreeMap<Integer, String> constrainedIndices = new TreeMap<Integer, String>();
		for (int i = 0; i < indexFields.size(); i++) {
			constrainedIndices.put(getIndexNumber(indexFields.get(i)), transformIndexValue(indexValues.get(i)));
		}
	
		return getReadersByIndices(this.directory, 0, constrainedIndices, new ArrayList<BufferedReader>());
	}
	
	private List<BufferedReader> getReadersByIndices(File curIndexDir, int curIndexNum, TreeMap<Integer, String> constrainedIndices, List<BufferedReader> readers) {
		File[] constrainedIndex = null;
		if (constrainedIndices.containsKey(curIndexNum)) {
			File constrainedIndexFile = new File(curIndexDir.getAbsolutePath(), constrainedIndices.get(curIndexNum));
			if (!constrainedIndexFile.exists())
				return readers;
			else 
				constrainedIndex = new File[] { constrainedIndexFile };
		} else {
			constrainedIndex = curIndexDir.listFiles();
		}
		
		for (File file : constrainedIndex) {
			if (file.isDirectory()) {
				getReadersByIndices(file, curIndexNum + 1, constrainedIndices, readers);
			} else if (file.isFile() && (constrainedIndices.size() == 0 || curIndexNum >= constrainedIndices.lastKey())) {
				readers.add(FileUtil.getFileReader(file.getAbsolutePath()));
			}
		}
		
		return readers;
	}
	
	@Override
	public boolean addItem(I item) {
		Serializer<I, S> serializer = getSerializer();
		List<Serializer.Index<I>> indices = serializer.getIndices();
		
		StringBuilder path = new StringBuilder();
		for (Serializer.Index<I> index : indices) {
			path.append(transformIndexValue(index.getValue(item))).append("/");
		}
		
		path = path.delete(path.length() - 1, path.length());
		
		File file = new File(this.directory.getAbsolutePath(), path.toString());
		
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(file));
			w.write(serializer.serializeToString(item));
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	@Override
	public boolean addItems(List<I> items) {
		for (I item : items) {
			if (!addItem(item))
				return false;
		}
		
		return true;
	}
	
	private String transformIndexValue(Object indexValue) {
		return indexValue.toString().replace("/", "_");
	}
	
	private int getIndexNumber(String indexField) {
		List<Serializer.Index<I>> indices = getSerializer().getIndices();
		int indexNum = -1;
		for (int i = 0; i < indices.size(); i++)
			if (indices.get(i).getField().equals(indexField))
				indexNum = i;
		return indexNum;
	}
	
	private StoreReference getStoreReference(File file) {
		List<String> fields = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();
		String indicesStr = file.getAbsolutePath().substring(this.directory.getAbsolutePath().length());
		if (indicesStr.startsWith("/"))
			indicesStr = indicesStr.substring(1);
		
		String[] indexValues = indicesStr.split("/");
		for (int i = 0; i < indexValues.length; i++) {
			fields.add(getSerializer().getIndices().get(i).getField());
			values.add(indexValues[i]);
		}
		
		if (this.storage == null) {
			return new StoreReference(null, this.name, fields, values);
		} else {
			return new StoreReference(this.storage.getName(), this.name, fields, values);
		}
	}

	@Override
	public Storage<?, S> getStorage() {
		return this.storage;
	}
}
