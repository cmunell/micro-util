package edu.cmu.ml.rtw.generic.data.store;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.util.FileUtil;

public class StorageFileSystem<S> implements Storage<StoredCollectionFileSystem<?, S>, S> {
	private static final String META_COLLECTION = "meta";
	
	private String name;
	private String rootDirectory;
	private Map<String, Serializer<?, ?>> serializers;
	
	public StorageFileSystem(String name, String rootDirectory, Map<String, Serializer<?, ?>> serializers) {
		this.name = name;
		this.rootDirectory = rootDirectory;
		this.serializers = serializers;
	}
	
	public String getName() {
		return this.name;
	}

	@Override
	public boolean hasCollection(String name) {
		File collectionFile = new File(this.rootDirectory, name);
		return collectionFile.exists() && collectionFile.isDirectory();
	}

	@Override
	public <I> StoredCollectionFileSystem<I, S> getCollection(String name) {
		if (!hasCollection(name))
			return null;
		
		return new StoredCollectionFileSystem<I, S>(name, new File(this.rootDirectory, name), this);	
	}
	
	@Override
	public <I> StoredCollectionFileSystem<?, S> getCollection(String name, Serializer<I, S> serializer) {
		if (!hasCollection(name))
			return null;
		
		return new StoredCollectionFileSystem<I, S>(name, new File(this.rootDirectory, name), serializer);
	}

	@Override
	public <I> StoredCollectionFileSystem<I, S> createCollection(String name, Serializer<I, S> serializer) {
		if (hasCollection(name))
			return null;
		
		File collectionDirectory = new File(this.rootDirectory, name);
		if (!collectionDirectory.mkdir())
			return null;
		
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(new File(this.rootDirectory, META_COLLECTION), true));
			w.write((new Document()).append("collection", name).append("serializer", serializer.getName()).toJson());
			w.newLine();
			w.close();
		} catch (IOException e) {
			collectionDirectory.delete();
			return null;
		}
		
		return new StoredCollectionFileSystem<I, S>(name, new File(this.rootDirectory, name), this, serializer);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <I> Serializer<I, S> getCollectionSerializer(String name) {
		try {
			BufferedReader r = FileUtil.getFileReader((new File(this.rootDirectory, META_COLLECTION)).getAbsolutePath());
			String line = null;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				Document document = Document.parse(line.trim());
				if (document.getString("collection").equals(name)) {
					r.close();
					return (Serializer<I, S>)this.serializers.get(document.getString("serializer"));
				}
			}
		
			r.close();
		
		} catch (IOException e) { }
		
		return null;
	}

	@Override
	public boolean renameCollection(String oldName, String newName) {
		if (!hasCollection(oldName) || hasCollection(newName) || oldName.equals(newName))
			return false;
		
		File oldDir = new File(this.rootDirectory, oldName);
		
		if (!updateSerializer(oldName, newName))
			return false;
		
		return oldDir.renameTo(new File(this.rootDirectory, newName));
	}

	@Override
	public boolean deleteCollection(String name) {
		if (!hasCollection(name))
			return false;
		
		File collectionDir = new File(this.rootDirectory, name);
		try {
			FileUtils.deleteDirectory(collectionDir);
		} catch (IOException e) {
			return false;
		}
	
		return updateSerializer(name, null);
	}
	
	private boolean updateSerializer(String collectionName, String newCollectionName) {
		try {
			BufferedReader r = FileUtil.getFileReader((new File(this.rootDirectory, META_COLLECTION)).getAbsolutePath());
			String line = null;
			List<Document> documents = new ArrayList<Document>();
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				Document document = Document.parse(line.trim());
				if (document.getString("collection").equals(collectionName)) {
					if (newCollectionName != null)
						document.put("collection", newCollectionName);
				} else {
					documents.add(document);
				}
			}
		
			r.close();
		
			BufferedWriter w = FileUtil.getFileWriter((new File(this.rootDirectory, META_COLLECTION)).getAbsolutePath());
			for (Document document : documents) {
				w.write(document.toJson());
				w.newLine();
			}
			
			w.close();
			
		} catch (IOException e) { 
			return false;
		}
		
		return true;
	}
}
