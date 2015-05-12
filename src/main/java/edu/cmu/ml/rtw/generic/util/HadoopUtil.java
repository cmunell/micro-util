package edu.cmu.ml.rtw.generic.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * HadoopUtil provides utilities for working with Hadoop.
 * 
 * @author Bill McDowell
 * 
 */
public class HadoopUtil {
	public static BufferedReader getFileReader(String path) {
		try {
			Path filePath = new Path(path);
			FileSystem fileSystem = FileSystem.get(new Configuration());
			BufferedReader reader = new BufferedReader(new InputStreamReader(fileSystem.open(filePath)));
			return reader;
		} catch (Exception e) {
			return null;
		}
	}
}
