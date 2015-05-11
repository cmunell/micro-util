/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of theMess (https://github.com/forkunited/theMess)
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

package edu.cmu.ml.rtw.generic.data.annotation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.JSONSerializable;

/**
 * 
 * Document represents a JSON-serializable text document with 
 * various NLP annotations (e.g. PoS tags, parses, etc).  The methods
 * for getting the NLP annotations are kept abstract so 
 * that they can be implemented in ways that allow for
 * caching in cases when all of the documents don't fit
 * in memory.  In-memory implementations of these methods
 * are given by the edu.cmu.ml.rtw.generic.data.annotation.DocumentInMemory 
 * class.
 * 
 * @author Bill McDowell
 *
 */
public abstract class Document implements JSONSerializable {
	protected DataTools dataTools;
	protected String name;
	
	public Document(DataTools dataTools) {
		this.dataTools = dataTools;
	}
	
	public Document(DataTools dataTools, JSONObject json) {
		this(dataTools);
		fromJSON(json);
	}
	
	public Document(DataTools dataTools, String jsonPath) {
		this(dataTools);
		BufferedReader r = FileUtil.getFileReader(jsonPath);
		String line = null;
		StringBuffer lines = new StringBuffer();
		try {
			while ((line = r.readLine()) != null) {
				lines.append(line).append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			fromJSON(new JSONObject(lines.toString()));
		} catch (JSONException e) {
			
		}
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean saveToJSONFile(String path) {
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(path));
			
			w.write(toJSON().toString());
			
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean meetsAnnotatorRequirements(AnnotationType<?>[] requirements) {
		for (AnnotationType<?> type : requirements)
			if (!hasAnnotationType(type))
				return false;
		return true;
	}
	
	public abstract Document makeInstanceFromJSONFile(String path);
	public abstract String getAnnotatorName(AnnotationType<?> annotationType);
	public abstract boolean hasAnnotationType(AnnotationType<?> annotationType);
	public abstract boolean hasConfidence(AnnotationType<?> annotationType);
	public abstract Collection<AnnotationType<?>> getAnnotationTypes();
}
