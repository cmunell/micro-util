package edu.cmu.ml.rtw.generic.util;

import java.io.File;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * Properties represents properties loaded from a properties file.  This
 * works the same as the java.util.Properties class, except that it allows
 * the use of environment variables in property values.  Properties classes
 * in other projects should extend this class to include their particular
 * properties. 
 * 
 * Note that the java.util.Properties class as intentionally hidden within 
 * this class instead of extended.  This prevents confusions between 
 * retrieving properties values using the typical java.util.Properties
 * methods and retrieving properties values using the methods of the
 * edu.cmu.ml.rtw.generic.util.Properties class.
 * 
 * @author Bill McDowell
 *
 */
public class Properties {
	protected java.util.Properties properties = null;
	protected Map<String, String> env = null;
	
	public Properties(String[] possiblePaths) {
		this(FileUtil.getPropertiesReader(possiblePaths));
	}
	
	public Properties(Reader reader) {
		try {
			this.properties = new java.util.Properties();
			this.properties.load(reader);
			this.env = System.getenv();
			
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected String loadProperty(String property) {
		String propertyValue = this.properties.getProperty(property);
		if (this.env != null) {
			for (Entry<String, String> envEntry : this.env.entrySet())
				propertyValue = propertyValue.replace("${" + envEntry.getKey() + "}", envEntry.getValue());
		}
		return propertyValue;
	}
	
	public String getContextDirectory() {
		if (this.properties.containsKey("context_dir"))
			return loadProperty("context_dir");
		else
			return null;
	}
	
	public Map<String, String> getFileSystemStorageBSONDirectories() {
		return getFileSystemStorageDirectories("bson");
	}
	
	public Map<String, String> getFileSystemStorageStringDirectories() {
		return getFileSystemStorageDirectories("str");
	}

	public Map<String, File> getWord2VecVectorFiles() {
		Map<String, File> vectorFiles = new HashMap<String, File>();
		Set<String> propertyKeys = this.properties.stringPropertyNames();
		
		for (String propertyKey : propertyKeys) {
			if (propertyKey.startsWith("word2vec_vectors_")) {
				String key = propertyKey.substring("word2vec_vectors_".length());
				if (key.length() == 0) {
					throw new IllegalArgumentException("Failed to load word2vec files");
				}
				
				vectorFiles.put(key, new File(loadProperty(propertyKey)));
			}
		}
		
		return vectorFiles;
	}
	
	public Map<String, File> getMateToolsModelFiles() {
		Map<String, File> mateFiles = new HashMap<String, File>();
		Set<String> propertyKeys = this.properties.stringPropertyNames();
		
		for (String propertyKey : propertyKeys) {
			if (propertyKey.startsWith("matetools_model_")) {
				String key = propertyKey.substring("matetools_model_".length());
				if (key.length() == 0) {
					throw new IllegalArgumentException("Failed to load matetools model files");
				}
				
				mateFiles.put(key, new File(loadProperty(propertyKey)));
			}
		}
		
		return mateFiles;
	}
	
	public File getWord2VecVectorFile() {
		Map<String, File> files = getWord2VecVectorFiles();
		if (files.size() > 1)
			throw new UnsupportedOperationException("Word2Vec vector file name not specified, but there is more than one possibility in the properties file");
		return files.values().iterator().next();
	}
	
	private Map<String, String> getFileSystemStorageDirectories(String type) {
		Map<String, String> info = new HashMap<String, String>();
		Set<String> propertyKeys = this.properties.stringPropertyNames();
		
		for (String propertyKey : propertyKeys) {
			if (propertyKey.startsWith("storage_fs_")) {
				String[] storageTypeAndName = propertyKey.substring("storage_fs_".length()).split("_");
				String storageType = storageTypeAndName[0];
				if (!storageType.equals(type))
					continue;
				
				String storageName = storageTypeAndName[1];
				String storageDir = loadProperty(propertyKey);
				info.put(storageName, storageDir);
			}
		}
		
		return info;
	}
	
	public String getDebugDirectory() {
		if (this.properties.containsKey("debug_dir"))
			return loadProperty("debug_dir");
		else
			return null;
	}
	
	public Integer getMaxThreads() {
		if (this.properties.containsKey("maxThreads"))
			return Integer.valueOf(this.properties.get("maxThreads").toString());
		else 
			return null;
	}
	
	public Integer getRandomSeed() {
		if (this.properties.containsKey("randomSeed"))
			return Integer.valueOf(this.properties.get("randomSeed").toString());
		else 
			return null;
	}
}
