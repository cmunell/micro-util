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
