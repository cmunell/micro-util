/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of ARKWater (https://github.com/forkunited/ARKWater)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

package edu.cmu.ml.rtw.generic.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

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
	
	public static BufferedReader getFileReader(String path) {
		InputStream resource = FileUtil.class.getClassLoader().getResourceAsStream(path);
		try { 
			if (resource != null)
				return new BufferedReader(new InputStreamReader(resource));
			File file = new File(path);
			if (file.exists())
				return new BufferedReader(new FileReader(file));
			
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
}
