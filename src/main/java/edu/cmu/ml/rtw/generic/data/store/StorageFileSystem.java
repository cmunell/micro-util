package edu.cmu.ml.rtw.generic.data.store;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.Serializer;

public class StorageFileSystem<S> implements Storage<StoredCollectionFileSystem<?, S>, S> {
	private static final String META_COLLECTION = "meta";
	
	private String rootDirectory;
	private Map<String, Serializer<?, ?>> serializers;
	
	public StorageFileSystem(String rootDirectory, Map<String, Serializer<?, ?>> serializers) {
		this.rootDirectory = rootDirectory;
		this.serializers = serializers;
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
		
		return new StoredCollectionFileSystem<I, S>(name, new File(this.rootDirectory, name), this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <I> Serializer<I, S> getCollectionSerializer(String name) {
		try {
			BufferedReader r = edu.cmu.ml.rtw.generic.util.FileUtil.getFileReader((new File(this.rootDirectory, META_COLLECTION)).getAbsolutePath());
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
}
