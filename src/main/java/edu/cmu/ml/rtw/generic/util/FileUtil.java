package edu.cmu.ml.rtw.generic.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.Serializer;

/**
 * 
 * FileUtil helps deal with files on a typical file system or 
 * HDFS (in Hadoop).  Given a path for which to read/write, it
 * determines whether the path points to an HDFS or local
 * file system, and then performs operations accordingly.
 * 
 * @author Bill McDowell
 *
 */
public class FileUtil {
	public static String readFile(File file) {
		return readFile(file.getAbsolutePath());
	}
	
	public static String readFile(String path) {
		BufferedReader reader = FileUtil.getFileReader(path);
		StringBuilder str = new StringBuilder();
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				str = str.append(line).append("\n");
			}
		} catch (IOException e) {
			return null;
		}
	
		return str.toString();
	}
	
	public static List<Map<String, String>> readSVFile(String path, String separator) {
		List<Map<String, String>> maps = new ArrayList<Map<String, String>>();
		BufferedReader r = getFileReader(path);
		if (r == null)
			return null;
		try {
			String keysLine = r.readLine();
			if (keysLine == null)
				return null;
			String[] keys = keysLine.split(separator);
			String line = null;
			while ((line = r.readLine()) != null) {
				String[] values = line.split(separator);
				if (keys.length != values.length)
					return null;
				
				Map<String, String> map = new HashMap<String, String>();
				for (int i = 0; i < keys.length; i++) {
					map.put(keys[i], values[i]);
				}
				maps.add(map);
			}
			
			r.close();
		} catch (IOException e) {
			return null;
		}
		
		return maps;
	}
	
	public static JSONObject readJSONFile(String path) {
		String str = readFile(path);
		try {
			return new JSONObject(str);
		} catch (JSONException e) {
			return null;
		}
	}
	
	public static JSONObject readJSONFile(File file) {
		return readJSONFile(file.getAbsolutePath());
	}

	public static <I, S> I readSerializedFile(Serializer<I, S> serializer, String path) {
		String str = readFile(path);
		return serializer.deserializeFromString(str);
	}
	
	public static <I, S> I readSerializedFile(Serializer<I, S> serializer, File file) {
		return readSerializedFile(serializer, file.getAbsolutePath());
	}
	
	public static BufferedReader getFileReader(String path) {
		try { 
			File file = new File(path);
			if (file.exists())
				return new BufferedReader(new FileReader(file));
			
			InputStream resource = FileUtil.class.getClassLoader().getResourceAsStream(path);
			
			if (resource != null)
				return new BufferedReader(new InputStreamReader(resource));
			
			
			System.err.println("WARNING: FileUtil failed to read file at " + path); // Do something better later
		} catch (Exception e) { System.err.println("WARNING: FileUtil failed to read file at " + path); e.printStackTrace(); }
		
		return HadoopUtil.getFileReader(path);
	}
	
	public static boolean fileExists(String path) {
		return FileUtil.class.getClassLoader().getResource(path) != null || (new File(path)).exists();
	}
	
	public static BufferedReader getFileReader(String path, String encoding) {
		File localFile = new File(path);
		try {
			if (localFile.exists())
				return new BufferedReader(new InputStreamReader(new FileInputStream(path), encoding));
			else 
				System.err.println("WARNING: FileUtil failed to read file at " + path); // Do something better later
		} catch (Exception e) { }
		
		return null;
	}
	
	public static BufferedReader getPropertiesReader(String[] possiblePaths) {
		for (int i = 0; i < possiblePaths.length; i++) {
			BufferedReader r = getFileReader(possiblePaths[i]);
			if (r != null)
				return r;
		}
		return null;
	}
	
	public static <I, S> boolean writeSerializedFile(Serializer<I, S> serializer, String path, I item) {		
		return writeFile(path, serializer.serializeToString(item));
	}
	
	public static <I, S> boolean writeSerializedFile(Serializer<I, S> serializer, File file, I item) {
		return writeSerializedFile(serializer, file.getAbsolutePath(), item);
	}
	
	public static boolean writeFile(String path, String str) {	
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(path));
			w.write(str);
			w.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public static boolean writeFile(File path, String str) {
		return writeFile(path.getAbsolutePath(), str);
	}
	
	public static BufferedWriter getFileWriter(String path) {
		try {
			return new BufferedWriter(new FileWriter(path));
		} catch (IOException e) {
			return null;
		}
	}
}
