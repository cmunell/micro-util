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
import java.util.Map;
import java.util.Map.Entry;

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
public abstract class Properties {
	protected java.util.Properties properties = null;
	protected Map<String, String> env = null;
	
	public Properties(String[] possiblePaths) {
		try {
			this.properties = new java.util.Properties();
			
			BufferedReader reader = FileUtil.getPropertiesReader(possiblePaths);
			this.properties.load(reader);
			reader.close();
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
}
